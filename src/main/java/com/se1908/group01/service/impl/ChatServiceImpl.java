package com.se1908.group01.service.impl;

import com.se1908.group01.dto.ChatAskRequest;
import com.se1908.group01.dto.ChatAskResponse;
import com.se1908.group01.dto.ChatSourceResponse;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.service.AiChatClientService;
import com.se1908.group01.service.AiGenerationOptionsService;
import com.se1908.group01.service.ChatService;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.PromptBuilderService;
import com.se1908.group01.service.SubscriptionEntitlementService;
import com.se1908.group01.service.VectorSearchService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
/**
 * Xử lý nhánh hỏi đáp single-document stateless qua POST /api/chat/ask.
 * Nhánh này đọc các chunk đã index, sinh embedding cho câu hỏi, gọi AI và chỉ ghi usage token.
 * Lịch sử session được xử lý bởi ChatSessionServiceImpl.
 */
public class ChatServiceImpl implements ChatService {

	// Single-document chỉ đưa tối đa 5 chunk gần câu hỏi nhất vào prompt để giới hạn token.
	private static final int TOP_K = 5;

	// Đọc danh tính user đã xác thực từ Security/JWT context.
	private final CurrentUserService currentUserService;
	// Kiểm tra document tồn tại, có quyền truy cập và đã ở trạng thái READY.
	private final DocumentAccessService documentAccessService;
	// Chuyển câu hỏi thành embedding vector bằng embedding provider.
	private final DocumentEmbeddingService documentEmbeddingService;
	// So sánh vector câu hỏi với vector chunk và trả về các chunk gần nhất.
	private final VectorSearchService vectorSearchService;
	// Ghép system rule, document context và câu hỏi thành prompt RAG.
	private final PromptBuilderService promptBuilderService;
	// Adapter gọi OpenAI Chat thông qua Spring AI.
	private final AiChatClientService aiChatClientService;
	// Kiểm tra và chuẩn hóa model/temperature từ request hoặc cấu hình mặc định.
	private final AiGenerationOptionsService aiGenerationOptionsService;
	// Kiểm tra quyền theo gói và ghi nhận token đã sử dụng.
	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public ChatServiceImpl(
			CurrentUserService currentUserService,
			DocumentAccessService documentAccessService,
			DocumentEmbeddingService documentEmbeddingService,
			VectorSearchService vectorSearchService,
			PromptBuilderService promptBuilderService,
			AiChatClientService aiChatClientService,
			AiGenerationOptionsService aiGenerationOptionsService,
			SubscriptionEntitlementService subscriptionEntitlementService
	) {
		this.currentUserService = currentUserService;
		this.documentAccessService = documentAccessService;
		this.documentEmbeddingService = documentEmbeddingService;
		this.vectorSearchService = vectorSearchService;
		this.promptBuilderService = promptBuilderService;
		this.aiChatClientService = aiChatClientService;
		this.aiGenerationOptionsService = aiGenerationOptionsService;
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	@Override
	public ChatAskResponse ask(ChatAskRequest request) {
		/**
		 * Hỏi AI trong phạm vi một document.
		 *
		 * Business flow: validate request -> kiểm tra document accessible/READY -> embedding câu hỏi
		 * -> cosine search tối đa 5 chunk -> enforce entitlement -> gọi AI -> ghi token usage -> trả sources.
		 */
		if (request == null) {
			throw new IllegalArgumentException("Chat request is required");
		}
		if (!StringUtils.hasText(request.getQuestion())) {
			throw new IllegalArgumentException("Question is required");
		}

		// User hiện tại được lấy từ JWT context, không tin userId do client tự gửi lên.
		var userId = currentUserService.getCurrentUserId();
		var generationOptions = aiGenerationOptionsService.resolve(
				request.getModel(),
				request.getTemperature()
		);
		// Chỉ document của user hoặc public document, chưa xóa và đã ingest READY mới được chat.
		var document = documentAccessService.getReadyDocumentForChat(userId, request.getDocumentId());
		// Embed nguyên câu hỏi để so sánh semantic với embedding của các chunk trong document.
		var queryVector = documentEmbeddingService.embedQuestion(request.getQuestion());
		// Lấy tối đa 5 chunk có cosine similarity cao nhất làm context cho prompt.
		var chunks = vectorSearchService.search(document.getDocumentId(), queryVector, TOP_K);
		if (chunks.isEmpty()) {
			// Không có chunk đã index thì không thể trả lời dựa trên document.
			throw new IllegalArgumentException("Document has no indexed content for chat");
		}

		// Prompt yêu cầu model chỉ dùng context document, không dùng kiến thức bên ngoài.
		var prompt = promptBuilderService.buildDocumentQuestionPrompt(request.getQuestion(), chunks);
		// Kiểm tra giới hạn chat/token theo subscription trước khi gọi provider bên ngoài.
		subscriptionEntitlementService.enforceAiRequestEntitlements(userId, 1, prompt);
		// Chỉ ghi usage sau khi provider sinh answer thành công.
		var answer = aiChatClientService.ask(prompt, generationOptions);
		subscriptionEntitlementService.recordAiTokenUsage(userId, prompt, answer);
		return new ChatAskResponse(
				document.getDocumentId(),
				answer,
				generationOptions.modelName(),
				generationOptions.temperature(),
				toSources(chunks)
		);
	}

	private List<ChatSourceResponse> toSources(List<RetrievedChunk> chunks) {
		// Chuyển kết quả nội bộ thành DTO citation; không trả content/vector thô của entity ra API.
		return chunks.stream()
				.map(retrieved -> {
					// Lấy entity chunk đi kèm similarity score trong RetrievedChunk.
					var chunk = retrieved.getChunk();
					return new ChatSourceResponse(
							// ID này cho phép client truy vết đúng chunk được dùng.
							chunk.getChunkId(),
							// Vị trí tuần tự của chunk trong document.
							chunk.getChunkIndex(),
							// Trang nguồn, có thể null nếu parser không xác định được.
							chunk.getPageNumber(),
							// Làm tròn score để response ổn định và dễ hiển thị.
							roundScore(retrieved.getScore())
					);
				})
				.toList();
	}

	private double roundScore(double score) {
		// Giữ tối đa bốn chữ số thập phân của cosine similarity.
		return Math.round(score * 10000.0) / 10000.0;
	}
}
