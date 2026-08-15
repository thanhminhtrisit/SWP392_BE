package com.se1908.group01.service.impl;

import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.MultiChatAskResponse;
import com.se1908.group01.dto.MultiChatSourceResponse;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.entity.Document;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.service.AiGenerationOptionsService;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.LlmClient;
import com.se1908.group01.service.MultiChatService;
import com.se1908.group01.service.PromptBuilderService;
import com.se1908.group01.service.SubscriptionEntitlementService;
import com.se1908.group01.service.VectorSearchService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
/**
 * Xử lý chat stateless trên nhiều tài liệu qua POST /api/chat/ask-multi.
 *
 * <p>Service chịu trách nhiệm xác định phạm vi tài liệu, embedding câu hỏi, lấy các
 * chunk gần nhất, xây dựng prompt RAG và gọi LLM. Luồng này chỉ trả kết quả về
 * client, không lưu lịch sử message như {@link ChatSessionServiceImpl}.</p>
 */
public class MultiChatServiceImpl implements MultiChatService {

	// Multi-document lấy nhiều context hơn single-document (10 thay vì 5 chunk).
	private static final int TOP_K = 10;
	// Preview chỉ dùng trong source response; prompt vẫn nhận đầy đủ nội dung chunk.
	private static final int CONTENT_PREVIEW_LENGTH = 200;

	private final DocumentAccessService documentAccessService;
	private final CurrentUserService currentUserService;
	private final DocumentEmbeddingService documentEmbeddingService;
	private final VectorSearchService vectorSearchService;
	private final PromptBuilderService promptBuilderService;
	private final LlmClient llmClient;
	private final AiGenerationOptionsService aiGenerationOptionsService;
	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public MultiChatServiceImpl(
			DocumentAccessService documentAccessService,
			CurrentUserService currentUserService,
			DocumentEmbeddingService documentEmbeddingService,
			VectorSearchService vectorSearchService,
			PromptBuilderService promptBuilderService,
			LlmClient llmClient,
			AiGenerationOptionsService aiGenerationOptionsService,
			SubscriptionEntitlementService subscriptionEntitlementService
	) {
		this.documentAccessService = documentAccessService;
		this.currentUserService = currentUserService;
		this.documentEmbeddingService = documentEmbeddingService;
		this.vectorSearchService = vectorSearchService;
		this.promptBuilderService = promptBuilderService;
		this.llmClient = llmClient;
		this.aiGenerationOptionsService = aiGenerationOptionsService;
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	@Override
	public MultiChatAskResponse askMulti(MultiChatAskRequest request) {
		// Validate quan hệ giữa mode và các field trước khi truy cập DB/provider.
		validateRequest(request);

		// userId luôn lấy từ JWT context, không nhận từ request để tránh truy cập chéo dữ liệu.
		var userId = currentUserService.getCurrentUserId();
		// Model/temperature chỉ ảnh hưởng bước sinh câu trả lời, không ảnh hưởng embedding/retrieval.
		var generationOptions = aiGenerationOptionsService.resolve(
				request.getModel(),
				request.getTemperature()
		);

		List<Document> documents;
		ChatMode chatMode;
		if ("SelectedDocuments".equals(request.getMode())) {
			chatMode = ChatMode.SELECTED_DOCUMENTS;
			// Phải resolve đủ tất cả document được chọn và tất cả đều accessible + READY.
			documents = documentAccessService.getReadyDocumentsForChat(userId, request.getSelectedDocumentIds());
		} else {
			chatMode = ChatMode.USER_STORAGE;
			// API stateless /ask-multi không có field useGeneralKnowledge nên cố định
			// false: UserStorage ở flow này chỉ tìm trong tài liệu thuộc user.
			documents = documentAccessService.getAllReadyDocumentsForUser(
					userId,
					request.getFolderId(),
					false
			);
		}
		var entitlementDocumentCount = chatMode == ChatMode.SELECTED_DOCUMENTS
				? request.getSelectedDocumentIds().size()
				: documents.size();
		// Chặn multi-document nếu subscription hiện tại không cho phép số document này.
		subscriptionEntitlementService.enforceDocumentChatEntitlement(userId, entitlementDocumentCount);

		// Chuyển entity thành danh sách ID cố định để giới hạn phạm vi vector search.
		var resolvedDocumentIds = documents.stream()
				.map(Document::getDocumentId)
				.toList();

		if (documents.isEmpty()) {
			// Không gọi embedding/LLM khi phạm vi không có tài liệu để tiết kiệm quota và token.
			return new MultiChatAskResponse(
					noContextMessage(chatMode, request.getFolderId()),
					chatMode.name(),
					generationOptions.modelName(),
					generationOptions.temperature(),
					List.of(),
					List.of()
			);
		}

		// Embed nguyên câu hỏi một lần rồi dùng cùng vector để so sánh với chunk của mọi document.
		var questionEmbeddingVector = documentEmbeddingService.embedQuestion(request.getQuestion());

		List<RetrievedChunk> chunks;
		if (chatMode == ChatMode.SELECTED_DOCUMENTS) {
			// Search chỉ trong chính danh sách document người dùng đã chọn.
			chunks = vectorSearchService.search(questionEmbeddingVector, resolvedDocumentIds, null, null, TOP_K);
		} else {
			// Pass userId so visibility filtering is applied correctly for UserStorage mode
			chunks = vectorSearchService.search(questionEmbeddingVector, resolvedDocumentIds, userId, request.getFolderId(), TOP_K);
		}

		if (chunks.isEmpty()) {
			// Empty ở đây nghĩa là không có chunk/vector hợp lệ; hiện tại search chưa áp similarity threshold.
			return new MultiChatAskResponse(
					noContextMessage(chatMode, request.getFolderId()),
					chatMode.name(),
					generationOptions.modelName(),
					generationOptions.temperature(),
					resolvedDocumentIds,
					List.of()
			);
		}

		// Context chứa full text của chunk; sources là metadata/preview riêng để FE hiển thị citation.
		var context = buildContext(chunks);
		var prompt = promptBuilderService.buildMultiDocumentQuestionPrompt(chatMode, context, request.getQuestion());
		var sources = buildSources(chunks);
		// Kiểm tra quota trước khi phát sinh chi phí gọi LLM.
		subscriptionEntitlementService.enforceAiTokenBudget(userId, prompt);
		var answer = llmClient.generateAnswer(prompt, generationOptions);
		// Chỉ ghi usage sau khi provider đã trả answer thành công.
		subscriptionEntitlementService.recordAiTokenUsage(userId, prompt, answer);

		// Response chỉ báo những document thật sự có chunk lọt vào TOP_K, không phải toàn bộ phạm vi tìm kiếm.
		var usedDocumentIds = chunks.stream()
				.map(rc -> rc.getChunk().getDocument().getDocumentId())
				.distinct()
				.toList();

		return new MultiChatAskResponse(
				answer,
				chatMode.name(),
				generationOptions.modelName(),
				generationOptions.temperature(),
				usedDocumentIds,
				sources
		);
	}

	private String noContextMessage(ChatMode mode, Long folderId) {
		// Thông báo được điều chỉnh theo đúng phạm vi mà user đã yêu cầu tìm kiếm.
		return switch (mode) {
			case SELECTED_DOCUMENTS ->
					"I cannot find this information in the documents you selected.";
			case USER_STORAGE -> folderId != null
					? "I cannot find sufficient information in this folder to answer this question."
					: "I cannot find sufficient information in your documents to answer this question.";
		};
	}

	private List<MultiChatSourceResponse> buildSources(List<RetrievedChunk> chunks) {
		// Giữ score và định danh chunk để FE có thể giải thích nguồn của câu trả lời.
		return chunks.stream()
				.map(retrieved -> {
					var chunk = retrieved.getChunk();
					return new MultiChatSourceResponse(
							chunk.getDocument().getDocumentId(),
							chunk.getDocument().getOriginalFileName(),
							chunk.getChunkId(),
							truncateForPreview(chunk.getContent()),
							retrieved.getScore()
					);
				})
				.toList();
	}

	private String truncateForPreview(String content) {
		// Không cắt content gốc trong DB hoặc context gửi LLM; chỉ cắt bản preview trả về API.
		if (content == null || content.length() <= CONTENT_PREVIEW_LENGTH) {
			return content;
		}
		return content.substring(0, CONTENT_PREVIEW_LENGTH) + "...";
	}

	private String buildContext(List<RetrievedChunk> chunks) {
		var sb = new StringBuilder();
		// Gắn documentId/chunkIndex trước nội dung để model phân biệt nguồn trong prompt nhiều tài liệu.
		for (var retrieved : chunks) {
			var chunk = retrieved.getChunk();
			sb.append("[Document ").append(chunk.getDocument().getDocumentId())
					.append(", chunk ").append(chunk.getChunkIndex()).append("]\n")
					.append(chunk.getContent())
					.append("\n\n");
		}
		return sb.toString().trim();
	}

	private void validateRequest(MultiChatAskRequest request) {
		// SelectedDocuments bắt buộc có danh sách ID; UserStorage không được mang danh sách chọn thủ công.
		if ("SelectedDocuments".equals(request.getMode())
				&& (request.getSelectedDocumentIds() == null || request.getSelectedDocumentIds().isEmpty())) {
			throw new IllegalArgumentException("selectedDocumentIds must be provided for SelectedDocuments mode");
		}
	}
}
