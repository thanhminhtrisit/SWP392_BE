package com.se1908.group01.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
/**
 * Tạo và serialize embedding vector cho các chunk tài liệu.
 * Service chia request thành batch và retry lỗi quota đã nhận diện trước khi ingestion thất bại.
 */
public class DocumentEmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(DocumentEmbeddingService.class);
	// Prefix khác nhau giúp embedding model phân biệt "đoạn tài liệu" và "truy vấn tìm kiếm".
	private static final String DOCUMENT_PREFIX = "title: none | text: ";
	private static final String QUESTION_PREFIX = "task: question answering | query: ";
	private static final int MAX_EMBEDDING_BATCH_SIZE = 90;
	private static final int MAX_RETRY_ATTEMPTS = 3;
	private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(30);
	private static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(90);
	private static final Pattern RETRY_INFO_PATTERN = Pattern.compile("retryDelay\"\\s*:\\s*\"(\\d+)s\"");
	private static final Pattern PLEASE_RETRY_PATTERN = Pattern.compile("Please retry in\\s+([0-9]+(?:\\.[0-9]+)?)s");

	private final EmbeddingModel embeddingModel;
	// ObjectMapper chuyển mảng số embedding thành JSON để lưu DB và đọc lại ổn định.
	private final ObjectMapper objectMapper;

	public DocumentEmbeddingService(@Nullable EmbeddingModel embeddingModel, ObjectMapper objectMapper) {
		// EmbeddingModel có thể null khi application chưa bật Spring AI embedding provider.
		this.embeddingModel = embeddingModel;
		this.objectMapper = objectMapper;
	}

	public String embedQuestion(String question) {
		// API chat dùng method này để biến câu hỏi thành cùng loại vector với document chunks.
		if (!StringUtils.hasText(question)) {
			throw new IllegalArgumentException("Question is required");
		}
		// Một câu hỏi cũng đi qua pipeline batch chung để dùng cùng cơ chế retry/serialize.
		var results = embedPreparedVectors(List.of(QUESTION_PREFIX + question));
		if (results.isEmpty()) {
			throw new IllegalStateException("Failed to embed question");
		}
		// Input chỉ có một câu hỏi nên vector đầu tiên chính là query vector cần tìm kiếm.
		return results.getFirst();
	}

	public List<String> embedVectors(List<String> texts) {
		if (texts == null || texts.isEmpty()) {
			return List.of();
		}

		// Chuẩn bị từng chunk theo định dạng document trước khi gửi batch sang embedding provider.
		var prepared = new ArrayList<String>(texts.size());
		for (String text : texts) {
			prepared.add(StringUtils.hasText(text) ? DOCUMENT_PREFIX + text : "");
		}
		return embedPreparedVectors(prepared);
	}

	private List<String> embedPreparedVectors(List<String> texts) {
		// Giữ nguyên thứ tự vector theo input để kết quả embedding luôn khớp đúng với chunk/câu hỏi ban đầu.
		if (embeddingModel == null) {
			// [SUA NGAY 2026-08-11 - co ho tro cua AI] Doi thong bao theo bien moi cua OpenAI.
			// Luu y ten bien doi tu SPRING_AI_MODEL_EMBEDDING_TEXT thanh SPRING_AI_MODEL_EMBEDDING
			// vi OpenAI dung thuoc tinh spring.ai.model.embedding (gia tri don), khong phai
			// spring.ai.model.embedding.text (map) nhu Google GenAI.
			throw new IllegalStateException("EmbeddingModel is not configured. Set SPRING_AI_MODEL_EMBEDDING=openai and OPENAI_API_KEY.");
		}

		var cleaned = new ArrayList<String>(texts.size());
		for (String t : texts) {
			// Thay input null/blank bằng chuỗi rỗng để giữ nguyên số lượng và vị trí phần tử.
			cleaned.add(StringUtils.hasText(t) ? t : "");
		}

		// Danh sách này giữ vector JSON đúng thứ tự với danh sách text đầu vào.
		List<String> vectors = new ArrayList<>(cleaned.size());
		for (int start = 0; start < cleaned.size(); start += MAX_EMBEDDING_BATCH_SIZE) {
			// Giữ request gửi provider dưới batch size đã cấu hình và bảo toàn thứ tự input.
			var end = Math.min(start + MAX_EMBEDDING_BATCH_SIZE, cleaned.size());
			// subList tạo view của đoạn [start, end), không sao chép hoặc đảo thứ tự dữ liệu.
			var batch = cleaned.subList(start, end);
			// Gọi provider và tự retry nếu lỗi được nhận diện là lỗi quota tạm thời.
			var response = embedBatchWithRetry(batch);
			var results = response.getResults();
			if (results == null || results.size() != batch.size()) {
				// Sai số lượng sẽ gán vector nhầm chunk, vì vậy phải dừng ingestion.
				throw new IllegalStateException("Embedding response size mismatch");
			}
			for (var r : results) {
				var output = r.getOutput();
				try {
					// Serialize float[] thành JSON để lưu trực tiếp vào cột embedding_vector.
					vectors.add(objectMapper.writeValueAsString(output));
				} catch (JsonProcessingException e) {
					throw new IllegalStateException("Failed to serialize embedding vector", e);
				}
			}
		}
		return vectors;
	}

	private EmbeddingResponse embedBatchWithRetry(List<String> batch) {
		// attempt chạy từ 0 nên tổng số lần gọi tối đa là 1 lần đầu + 3 lần retry.
		for (int attempt = 0; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				// Spring AI gửi toàn bộ batch sang OpenAI Embedding và trả vectors cùng thứ tự.
				return embeddingModel.embedForResponse(batch);
			} catch (RuntimeException ex) {
				// Lỗi không phải quota hoặc đã hết số lần retry thì trả lỗi ngay cho tầng gọi.
				if (!isQuotaError(ex) || attempt >= MAX_RETRY_ATTEMPTS) {
					throw ex;
				}
				// Tôn trọng retry delay của provider khi quota bị vượt trước khi thử lại batch.
				var delay = extractRetryDelay(ex);
				log.warn(
						"OpenAI embedding quota reached. Retrying batch in {} seconds. attempt={}/{} batchSize={}",
						delay.toSeconds(),
						attempt + 1,
						MAX_RETRY_ATTEMPTS,
						batch.size()
				);
				sleep(delay);
			}
		}
		throw new IllegalStateException("Embedding retry attempts exhausted");
	}

	private boolean isQuotaError(RuntimeException ex) {
		// Provider không expose một exception quota thống nhất nên code nhận diện qua message.
		var message = ex.getMessage();
		if (!StringUtils.hasText(message)) {
			return false;
		}
		return message.contains("429")
				|| message.contains("Quota exceeded")
				|| message.contains("retryDelay")
				|| message.contains("Please retry in");
	}

	private Duration extractRetryDelay(RuntimeException ex) {
		// Ưu tiên thời gian retry do provider trả về thay vì retry liên tục.
		var message = ex.getMessage();
		if (!StringUtils.hasText(message)) {
			return DEFAULT_RETRY_DELAY;
		}

		var retryInfoMatcher = RETRY_INFO_PATTERN.matcher(message);
		if (retryInfoMatcher.find()) {
			// Cộng một giây đệm rồi clamp để tránh retry sớm hơn thời điểm provider cho phép.
			return clampRetryDelay(Duration.ofSeconds(Long.parseLong(retryInfoMatcher.group(1)) + 1));
		}

		var pleaseRetryMatcher = PLEASE_RETRY_PATTERN.matcher(message);
		if (pleaseRetryMatcher.find()) {
			// Làm tròn lên vì message có thể trả số giây dạng thập phân.
			var seconds = Math.ceil(Double.parseDouble(pleaseRetryMatcher.group(1)));
			return clampRetryDelay(Duration.ofSeconds((long) seconds + 1));
		}

		return DEFAULT_RETRY_DELAY;
	}

	private Duration clampRetryDelay(Duration delay) {
		// Delay không hợp lệ được thay bằng giá trị mặc định an toàn.
		if (delay.isNegative() || delay.isZero()) {
			return DEFAULT_RETRY_DELAY;
		}
		// Không giữ request thread ngủ quá giới hạn 90 giây cho một lần retry.
		if (delay.compareTo(MAX_RETRY_DELAY) > 0) {
			return MAX_RETRY_DELAY;
		}
		return delay;
	}

	private void sleep(Duration delay) {
		try {
			// Tạm dừng đúng thời gian quota yêu cầu trước khi vòng lặp thử lại batch.
			Thread.sleep(delay.toMillis());
		} catch (InterruptedException ex) {
			// Khôi phục interrupt flag để tầng runtime biết thread đã bị yêu cầu dừng.
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Embedding retry was interrupted", ex);
		}
	}
}
