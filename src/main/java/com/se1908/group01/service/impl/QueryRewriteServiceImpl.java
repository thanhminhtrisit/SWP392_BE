package com.se1908.group01.service.impl;

import com.se1908.group01.config.RagProperties;
import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.service.LlmClient;
import com.se1908.group01.service.QueryRewriteService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * [THEM MOI 2026-08-22 - co ho tro cua AI]
 *
 * NGUYEN TAC THIET KE: day la tinh nang TANG CUONG, khong phai tinh nang thiet yeu.
 * Moi duong loi deu phai rot ve cau hoi goc thay vi lam chet luong chat. Mot buoc phu
 * khong duoc phep pha hong viec chinh.
 */
@Service
public class QueryRewriteServiceImpl implements QueryRewriteService {

	private static final Logger log = LoggerFactory.getLogger(QueryRewriteServiceImpl.class);

	// Cau viet lai dai hon nguong nay gan nhu chac chan la model da tra ve mot doan giai
	// thich thay vi mot cau hoi. Coi la that bai va dung cau goc.
	private static final int MAX_REWRITTEN_LENGTH = 400;

	// Cat bot cau tra loi cu dai dong khi dua vao prompt viet lai. Muc dich cua prompt nay
	// chi la giai nghia dai tu, khong can toan van noi dung - ngan thi re hon va nhanh hon.
	private static final int MAX_MEMORY_TEXT_LENGTH = 500;

	private final LlmClient llmClient;
	private final RagProperties ragProperties;

	public QueryRewriteServiceImpl(LlmClient llmClient, RagProperties ragProperties) {
		this.llmClient = llmClient;
		this.ragProperties = ragProperties;
	}

	@Override
	public String rewrite(
			String question,
			List<Message> conversationMemory,
			AiGenerationOptions options
	) {
		if (!StringUtils.hasText(question)) {
			return question;
		}
		var config = ragProperties.getQueryRewrite();
		if (!config.isEnabled()) {
			// Co tat: giu nguyen hanh vi cu. Dung de so sanh A/B luc demo ma khong can build lai.
			return question;
		}
		if (conversationMemory == null || conversationMemory.isEmpty()) {
			// Luot dau tien cua session: khong co gi de tham chieu nguoc.
			// Thoat som o day la ly do chi phi chi tang khoang 10% chu khong phai gap doi.
			return question;
		}

		try {
			var recent = tail(conversationMemory, config.getMaxMemoryMessages());
			var rewritten = sanitize(llmClient.generateAnswer(buildPrompt(question, recent), options));
			if (!StringUtils.hasText(rewritten)) {
				log.warn("Query rewrite tra ve chuoi rong, dung cau hoi goc.");
				return question;
			}
			if (rewritten.length() > MAX_REWRITTEN_LENGTH) {
				log.warn(
						"Query rewrite tra ve {} ky tu (nguong {}), co ve la doan giai thich chu"
								+ " khong phai cau hoi. Dung cau hoi goc.",
						rewritten.length(),
						MAX_REWRITTEN_LENGTH
				);
				return question;
			}
			if (!rewritten.equals(question)) {
				log.debug("Query rewrite: [{}] -> [{}]", question, rewritten);
			}
			return rewritten;
		} catch (RuntimeException ex) {
			// LLM timeout, het quota, tra ve rac... deu roi vao day. Chat van chay binh thuong.
			log.warn("Query rewrite that bai, dung cau hoi goc. Nguyen nhan: {}", ex.toString());
			return question;
		}
	}

	/** Chi lay {@code limit} tin nhan cuoi. Cang nhieu lich su cang de viet lai lech chu de. */
	private List<Message> tail(List<Message> messages, int limit) {
		if (limit <= 0 || messages.size() <= limit) {
			return messages;
		}
		return messages.subList(messages.size() - limit, messages.size());
	}

	private String buildPrompt(String question, List<Message> recent) {
		var prompt = new StringBuilder();
		// LUU Y: rieng phan prompt duoi day viet tieng Viet CO DAU day du - khac quy uoc
		// comment khong dau cua du an. Day la van ban gui cho model doc, viet dung chinh ta
		// cho ket qua tot hon han.
		prompt.append("""
				Bạn là bộ viết lại truy vấn cho một hệ thống tìm kiếm tài liệu.
				Nhiệm vụ: dựa vào lịch sử hội thoại, viết lại CÂU HỎI MỚI thành một câu hỏi ĐỘC LẬP,
				tự nó đủ nghĩa mà không cần đọc lại lịch sử.

				QUY TẮC BẮT BUỘC:
				1. Thay mọi đại từ và tham chiếu ngược ("người đó", "cái này", "người thứ hai",
				   "còn ai nữa") bằng danh từ cụ thể lấy từ lịch sử.
				2. Nếu CÂU HỎI MỚI đã tự đủ nghĩa, trả về NGUYÊN VĂN câu hỏi đó, không thêm bớt.
				3. Nếu CÂU HỎI MỚI nói về chủ đề khác hẳn lịch sử, trả về NGUYÊN VĂN câu hỏi đó.
				   Tuyệt đối không kéo chủ đề cũ vào.
				4. Giữ nguyên ngôn ngữ của câu hỏi gốc.
				5. CHỈ trả về đúng một câu hỏi. Không giải thích, không xuống dòng, không đặt trong
				   dấu ngoặc kép, không thêm tiền tố như "Câu hỏi:".

				Lịch sử hội thoại:
				""");
		for (var message : recent) {
			prompt.append("- ")
					.append(message.getMessageType().name())
					.append(": ")
					.append(truncate(message.getText()))
					.append('\n');
		}
		prompt.append("\nCÂU HỎI MỚI: ").append(question).append("\n\nCâu hỏi độc lập:");
		return prompt.toString();
	}

	private String truncate(String text) {
		if (text == null) {
			return "";
		}
		var trimmed = text.strip();
		return trimmed.length() <= MAX_MEMORY_TEXT_LENGTH
				? trimmed
				: trimmed.substring(0, MAX_MEMORY_TEXT_LENGTH) + "...";
	}

	/**
	 * Model doi khi tra ve kem dau ngoac kep, tien to, hoac vai dong giai thich du da duoc
	 * dan la dung lam vay. Chi giu dong dau tien va boc lop dau ngoac neu co.
	 */
	private String sanitize(String raw) {
		if (raw == null) {
			return null;
		}
		var text = raw.strip();
		var newline = text.indexOf('\n');
		if (newline >= 0) {
			text = text.substring(0, newline).strip();
		}
		if (text.length() >= 2
				&& (text.startsWith("\"") && text.endsWith("\"")
						|| text.startsWith("'") && text.endsWith("'"))) {
			text = text.substring(1, text.length() - 1).strip();
		}
		return text;
	}
}
