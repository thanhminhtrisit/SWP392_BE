package com.se1908.group01.service.impl;

import com.se1908.group01.config.RagProperties;
import com.se1908.group01.dto.ChatMessageListResponse;
import com.se1908.group01.dto.ChatMessageResponse;
import com.se1908.group01.dto.ChatMessageSourceResponse;
import com.se1908.group01.dto.ChatSessionResponse;
import com.se1908.group01.dto.CreateChatSessionRequest;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.dto.SendChatMessageRequest;
import com.se1908.group01.dto.UpdateChatSessionRequest;
import com.se1908.group01.entity.ChatMessage;
import com.se1908.group01.entity.ChatMessageSource;
import com.se1908.group01.entity.ChatSession;
import com.se1908.group01.entity.ChatSessionDocument;
import com.se1908.group01.entity.ChatSessionDocumentId;
import com.se1908.group01.entity.Document;
import com.se1908.group01.enums.ChatMessageRole;
import com.se1908.group01.enums.ChatMessageStatus;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.ChatMessageRepository;
import com.se1908.group01.repository.ChatMessageSourceRepository;
import com.se1908.group01.repository.ChatSessionDocumentRepository;
import com.se1908.group01.repository.ChatSessionRepository;
import com.se1908.group01.service.AiGenerationOptionsService;
import com.se1908.group01.service.ChatConversationMemoryService;
import com.se1908.group01.service.ChatSessionService;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.LlmClient;
import com.se1908.group01.service.PromptBuilderService;
import com.se1908.group01.service.QueryRewriteService;
import com.se1908.group01.service.SubscriptionEntitlementService;
import com.se1908.group01.service.VectorSearchService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
/**
 * Xử lý persistent chat session, là nhánh chính của DocumentChat.
 * Với single-document, session có mode SELECTED_DOCUMENTS và liên kết đúng một document.
 * Service quản lý cả RAG, conversation memory, lưu message/source và trả response cho controller.
 */
public class ChatSessionServiceImpl implements ChatSessionService {

	private static final int TOP_K = 10;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String DEFAULT_TITLE = "New chat";

	private final CurrentUserService currentUserService;
	private final DocumentAccessService documentAccessService;
	private final DocumentEmbeddingService documentEmbeddingService;
	private final VectorSearchService vectorSearchService;
	private final PromptBuilderService promptBuilderService;
	private final LlmClient llmClient;
	private final AiGenerationOptionsService aiGenerationOptionsService;
	private final ChatConversationMemoryService chatConversationMemoryService;
	// [THEM MOI 2026-08-22 - co ho tro cua AI] Viet lai cau hoi follow-up truoc khi embed.
	private final QueryRewriteService queryRewriteService;
	private final ChatSessionRepository chatSessionRepository;
	private final ChatSessionDocumentRepository chatSessionDocumentRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatMessageSourceRepository chatMessageSourceRepository;
	private final RagProperties ragProperties;
	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public ChatSessionServiceImpl(
			CurrentUserService currentUserService,
			DocumentAccessService documentAccessService,
			DocumentEmbeddingService documentEmbeddingService,
			VectorSearchService vectorSearchService,
			PromptBuilderService promptBuilderService,
			LlmClient llmClient,
			AiGenerationOptionsService aiGenerationOptionsService,
			ChatConversationMemoryService chatConversationMemoryService,
			// [THEM MOI 2026-08-22 - co ho tro cua AI] Tham so moi. Test cu phai cap nhat theo.
			QueryRewriteService queryRewriteService,
			ChatSessionRepository chatSessionRepository,
			ChatSessionDocumentRepository chatSessionDocumentRepository,
			ChatMessageRepository chatMessageRepository,
			ChatMessageSourceRepository chatMessageSourceRepository,
			RagProperties ragProperties,
			SubscriptionEntitlementService subscriptionEntitlementService
	) {
		this.currentUserService = currentUserService;
		this.documentAccessService = documentAccessService;
		this.documentEmbeddingService = documentEmbeddingService;
		this.vectorSearchService = vectorSearchService;
		this.promptBuilderService = promptBuilderService;
		this.llmClient = llmClient;
		this.aiGenerationOptionsService = aiGenerationOptionsService;
		this.chatConversationMemoryService = chatConversationMemoryService;
		this.queryRewriteService = queryRewriteService;
		this.chatSessionRepository = chatSessionRepository;
		this.chatSessionDocumentRepository = chatSessionDocumentRepository;
		this.chatMessageRepository = chatMessageRepository;
		this.chatMessageSourceRepository = chatMessageSourceRepository;
		this.ragProperties = ragProperties;
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	@Transactional
	@Override
	public ChatSessionResponse createSession(CreateChatSessionRequest request) {
		/**
		 * Tạo session và liên kết document được chọn.
		 * @Transactional bảo đảm việc lưu chat_session và chat_session_document cùng nằm trong một transaction.
		 */
		var userId = currentUserService.getCurrentUserId();
		var mode = resolveMode(request.mode());
		var policy = resolvePolicy(mode, request.useGeneralKnowledge());
		var options = aiGenerationOptionsService.resolve(request.model(), request.temperature());
		List<Document> selectedDocuments = List.of();
		List<Document> userStorageDocuments = List.of();

		if (mode == ChatMode.SELECTED_DOCUMENTS) {
			// Single-document đi vào nhánh này; folderId bị cấm và danh sách phải chứa document READY accessible.
			if (request.folderId() != null) {
				throw new IllegalArgumentException("folderId is not supported in SelectedDocuments mode");
			}
			selectedDocuments = documentAccessService.getReadyDocumentsForChat(
					userId,
					request.selectedDocumentIds()
			);
		} else {
			if (request.selectedDocumentIds() != null && !request.selectedDocumentIds().isEmpty()) {
				throw new IllegalArgumentException("selectedDocumentIds is not supported in UserStorage mode");
			}
			// useGeneralKnowledge được đổi thành KnowledgePolicy; policy PLUS_GENERAL
			// cũng là công tắc cho phép đưa public documents vào phạm vi tìm kiếm.
			userStorageDocuments = documentAccessService.getAllReadyDocumentsForUser(
					userId,
					request.folderId(),
					policy == KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
			);
		}
		// Plan được kiểm tra trước khi tạo session để chặn plan không cho phép nhiều document.
		var entitlementDocumentCount = mode == ChatMode.SELECTED_DOCUMENTS
				? selectedDocuments.size()
				: userStorageDocuments.size();
		subscriptionEntitlementService.enforceDocumentChatEntitlement(userId, entitlementDocumentCount);

		// Lưu policy/model/temperature vào session để các message sau dùng đúng cấu hình ban đầu.
		var session = new ChatSession();
		session.setUserId(userId);
		session.setTitle(normalizeTitle(request.title()));
		session.setChatMode(mode);
		session.setFolderId(request.folderId());
		session.setKnowledgePolicy(policy);
		session.setModel(options.modelName());
		session.setTemperature(options.temperature());
		session = chatSessionRepository.save(session);

		// Lưu bảng liên kết; single-document tạo đúng một dòng chat_session_document.
		for (var document : selectedDocuments) {
			var link = new ChatSessionDocument();
			link.setId(new ChatSessionDocumentId(session.getSessionId(), document.getDocumentId()));
			link.setChatSession(session);
			link.setDocument(document);
			chatSessionDocumentRepository.save(link);
		}

		return toSessionResponse(session);
	}

	@Transactional(readOnly = true)
	@Override
	public List<ChatSessionResponse> getMySessions() {
		// FE dùng danh sách này để tìm session có selectedDocumentIds đúng document đang mở.
		var userId = currentUserService.getCurrentUserId();
		return chatSessionRepository.findByUserIdAndIsDeletedFalseOrderByUpdatedAtDesc(userId)
				.stream()
				.map(this::toSessionResponse)
				.toList();
	}

	@Transactional
	@Override
	public ChatSessionResponse updateSession(Long sessionId, UpdateChatSessionRequest request) {
		var session = findOwnedSession(sessionId);
		session.setTitle(normalizeTitle(request.title()));
		if (request.useGeneralKnowledge() != null) {
			if (session.getChatMode() != ChatMode.USER_STORAGE) {
				throw new IllegalArgumentException(
						"General knowledge can only be changed in UserStorage mode"
				);
			}

			var policy = request.useGeneralKnowledge()
					? KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
					: KnowledgePolicy.DOCUMENTS_ONLY;
			session.setKnowledgePolicy(policy);
		}
		return toSessionResponse(chatSessionRepository.save(session));
	}

	@Transactional
	@Override
	public void deleteSession(Long sessionId) {
		var session = findOwnedSession(sessionId);
		session.setIsDeleted(Boolean.TRUE);
		chatSessionRepository.save(session);
	}

	@Transactional(readOnly = true)
	@Override
	public ChatMessageListResponse getMessages(Long sessionId, int page, int size) {
		// Chỉ đọc session thuộc user hiện tại, phân trang message và ghép source citation theo messageId.
		var session = findOwnedSession(sessionId);
		if (page < 0) {
			throw new IllegalArgumentException("Page must be greater than or equal to 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE);
		}

		var messagePage = chatMessageRepository.findByChatSessionSessionIdOrderByCreatedAtAscMessageIdAsc(
				session.getSessionId(),
				PageRequest.of(page, size)
		);
		var messageIds = messagePage.getContent().stream()
				.map(ChatMessage::getMessageId)
				.toList();
		Map<Long, List<ChatMessageSourceResponse>> sourcesByMessage = loadSources(messageIds);
		var messages = messagePage.getContent().stream()
				.map(message -> toMessageResponse(
						message,
						sourcesByMessage.getOrDefault(message.getMessageId(), List.of())
				))
				.toList();

		return new ChatMessageListResponse(
				messages,
				messagePage.getNumber(),
				messagePage.getSize(),
				messagePage.getTotalElements(),
				messagePage.getTotalPages()
		);
	}

	@Override
	public ChatMessageResponse sendMessage(Long sessionId, SendChatMessageRequest request) {
		/**
		 * Xử lý một câu hỏi trong persistent session.
		 * Runtime flow: kiểm tra session -> lấy memory -> lưu USER message -> resolve document READY
		 * -> embed/search -> build prompt -> kiểm tra token -> gọi LLM -> lưu ASSISTANT message/sources.
		 */
		var session = findOwnedSession(sessionId);
		var requestedModel = request.model() != null ? request.model() : session.getModel();
		var requestedTemperature = request.temperature() != null
				? request.temperature()
				: session.getTemperature();
		var options = aiGenerationOptionsService.resolve(requestedModel, requestedTemperature);
		var sessionChanged = false;
		if (!options.modelName().equals(session.getModel())) {
			session.setModel(options.modelName());
			sessionChanged = true;
		}
		if (!Double.valueOf(options.temperature()).equals(session.getTemperature())) {
			session.setTemperature(options.temperature());
			sessionChanged = true;
		}
		if (request.useGeneralKnowledge() != null) {
			if (session.getChatMode() != ChatMode.USER_STORAGE) {
				throw new IllegalArgumentException(
						"General knowledge can only be changed in UserStorage mode"
				);
			}
			var requestedPolicy = request.useGeneralKnowledge()
					? KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
					: KnowledgePolicy.DOCUMENTS_ONLY;
			if (requestedPolicy != session.getKnowledgePolicy()) {
				session.setKnowledgePolicy(requestedPolicy);
				sessionChanged = true;
			}
		}
		if (sessionChanged) {
			chatSessionRepository.save(session);
		}
		// Memory chỉ lấy các message COMPLETED gần nhất để hiểu câu hỏi follow-up.
		var conversationMemory = chatConversationMemoryService.getRecentMessages(sessionId);
		// Lưu câu hỏi trước khi gọi AI để lịch sử vẫn ghi nhận user đã gửi gì.
		saveMessage(
				session,
				ChatMessageRole.USER,
				request.question().trim(),
				ChatMessageStatus.COMPLETED
		);

		try {
			// Resolve lại document ở backend, tránh tin danh sách document cũ mà session từng lưu.
			var documents = resolveDocuments(session);
			// Kiểm tra entitlement lần nữa ở thời điểm gửi message, không chỉ lúc tạo session.
			subscriptionEntitlementService.enforceDocumentChatEntitlement(session.getUserId(), documents.size());
			if (documents.isEmpty()) {
				// Không còn document accessible thì lưu câu trả lời có kiểm soát, không gọi LLM.
				return saveNoContextAnswer(session);
			}

			var resolvedDocumentIds = documents.stream()
					.map(Document::getDocumentId)
					.toList();
			// [SUA NGAY 2026-08-22 - co ho tro cua AI] THEM buoc viet lai truy van.
			//
			// TRUOC DAY: embed thang request.question(). conversationMemory duoc lay o tren
			// nhung CHI dung khi dung prompt, khong he di vao cau tim kiem. Hau qua la tu cau
			// hoi thu hai tro di ("nguoi thu hai phu trach gi?") thi truy hoi mu hoan toan.
			//
			// BAY GIO: viet lai thanh cau hoi doc lap roi moi embed. Cau GOC van duoc luu vao
			// lich su (o tren) va van di vao prompt (o duoi) - chi rieng chuoi dem di tim kiem
			// la ban viet lai.
			//
			// AN TOAN: queryRewriteService khong bao gio nem exception. Lich su rong, co tat,
			// hay LLM loi deu tra ve cau hoi goc. Neu no hong thi hanh vi quay ve dung nhu cu.
			var searchQuery = queryRewriteService.rewrite(
					request.question(),
					conversationMemory,
					options
			);
			// Embed câu hỏi (đã viết lại nếu là follow-up); source hiện tại không có bước dịch
			// tiếng Việt sang tiếng Anh.
			var questionEmbedding = documentEmbeddingService.embedQuestion(searchQuery);
			var chunks = vectorSearchService.search(
					questionEmbedding,
					resolvedDocumentIds,
					session.getUserId(),
					session.getFolderId(),
					TOP_K
			);
			if (chunks.isEmpty()) {
				// Không tìm thấy context phù hợp thì trả thông báo nghiệp vụ thay vì đoán câu trả lời.
				return saveNoContextAnswer(session);
			}

			// Prompt gồm policy documents-only, memory hội thoại, context chunk và câu hỏi hiện tại.
			var prompt = promptBuilderService.buildSessionQuestionPrompt(
					session.getChatMode(),
					session.getKnowledgePolicy(),
					buildContext(chunks),
					conversationMemory,
					request.question()
			);
			// Chặn trước khi gọi provider nếu monthly token budget của plan đã hết.
			subscriptionEntitlementService.enforceAiTokenBudget(session.getUserId(), prompt);
			var answer = llmClient.generateAnswer(prompt, options);
			// Ghi usage sau khi LLM trả lời thành công; token là giá trị ước lượng theo độ dài text.
			subscriptionEntitlementService.recordAiTokenUsage(session.getUserId(), prompt, answer);
			var assistantMessage = saveMessage(
					session,
					ChatMessageRole.ASSISTANT,
					answer,
					ChatMessageStatus.COMPLETED
			);
			var sources = saveSources(assistantMessage, chunks);
			touchSession(session);
			// [SUA NGAY 2026-08-22 - co ho tro cua AI] Tra kem searchQuery de kiem chung duoc
			// query rewriting bang Postman ma khong phai doc log server.
			return toMessageResponse(assistantMessage, sources, searchQuery);
		} catch (RuntimeException ex) {
			// Nếu lỗi sau khi đã ghi USER message, lưu ASSISTANT FAILED để lịch sử phản ánh lần gọi lỗi.
			saveMessage(
					session,
					ChatMessageRole.ASSISTANT,
					"AI service could not generate a response.",
					ChatMessageStatus.FAILED
			);
			touchSession(session);
			throw ex;
		}
	}

	private List<Document> resolveDocuments(ChatSession session) {
		if (session.getChatMode() == ChatMode.SELECTED_DOCUMENTS) {
			// Single-document lấy documentId từ bảng liên kết session-document rồi kiểm tra READY/access lần nữa.
			var documentIds = chatSessionDocumentRepository.findDocumentIdsBySessionId(session.getSessionId());
			if (documentIds.isEmpty()) {
				return List.of();
			}
			return documentAccessService.getReadyDocumentsForChat(session.getUserId(), documentIds);
		}
		// UserStorage không nhận lại lựa chọn từ request gửi message. Backend dùng
		// KnowledgePolicy đã lưu khi tạo session để quyết định có lấy public hay không.
		return documentAccessService.getAllReadyDocumentsForUser(
				session.getUserId(),
				session.getFolderId(),
				session.getKnowledgePolicy() == KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
		);
	}

	private ChatMessageResponse saveNoContextAnswer(ChatSession session) {
		// Câu trả lời này vẫn được lưu COMPLETED để FE hiển thị và lần sau không gọi LLM vô ích.
		var answer = noContextMessage(session.getChatMode(), session.getKnowledgePolicy());
		var assistantMessage = saveMessage(
				session,
				ChatMessageRole.ASSISTANT,
				answer,
				ChatMessageStatus.COMPLETED
		);
		touchSession(session);
		return toMessageResponse(assistantMessage, List.of());
	}

	private ChatMessage saveMessage(
			ChatSession session,
			ChatMessageRole role,
			String content,
			ChatMessageStatus status
	) {
		var message = new ChatMessage();
		message.setChatSession(session);
		message.setRole(role);
		message.setContent(content);
		message.setStatus(status);
		return chatMessageRepository.save(message);
	}

	private List<ChatMessageSourceResponse> saveSources(
			ChatMessage message,
			List<RetrievedChunk> chunks
	) {
		List<ChatMessageSource> entities = new ArrayList<>();
		// Mỗi chunk được dùng làm context tạo một source record để trả citation về UI.
		for (var retrieved : chunks) {
			var chunk = retrieved.getChunk();
			var source = new ChatMessageSource();
			source.setMessage(message);
			source.setDocumentId(chunk.getDocument().getDocumentId());
			source.setChunkId(chunk.getChunkId());
			source.setPageNumber(chunk.getPageNumber());
			source.setSimilarityScore(roundScore(retrieved.getScore()));
			entities.add(source);
		}
		chatMessageSourceRepository.saveAll(entities);
		return entities.stream()
				.map(this::toSourceResponse)
				.toList();
	}

	private Map<Long, List<ChatMessageSourceResponse>> loadSources(List<Long> messageIds) {
		// Nạp source theo batch messageIds để response lịch sử có citation mà không query từng message.
		Map<Long, List<ChatMessageSourceResponse>> result = new HashMap<>();
		if (messageIds.isEmpty()) {
			return result;
		}
		for (var source : chatMessageSourceRepository.findByMessageMessageIdIn(messageIds)) {
			result.computeIfAbsent(source.getMessage().getMessageId(), ignored -> new ArrayList<>())
					.add(toSourceResponse(source));
		}
		return result;
	}

	// [SUA NGAY 2026-08-22 - co ho tro cua AI] Ban 2 tham so nay duoc GIU LAI de moi loi goi
	// cu (doc lich su tin nhan) khong phai sua. No uy quyen sang ban 3 tham so voi
	// rewrittenQuestion = null - dung ngu nghia mong muon, vi luc doc lai lich su thi khong
	// con khai niem "cau vua duoc tim kiem".
	private ChatMessageResponse toMessageResponse(
			ChatMessage message,
			List<ChatMessageSourceResponse> sources
	) {
		return toMessageResponse(message, sources, null);
	}

	private ChatMessageResponse toMessageResponse(
			ChatMessage message,
			List<ChatMessageSourceResponse> sources,
			String rewrittenQuestion
	) {
		// Chuẩn hóa entity message và source thành DTO public, không trả trực tiếp entity JPA ra API.
		return new ChatMessageResponse(
				message.getMessageId(),
				message.getRole(),
				message.getContent(),
				message.getStatus(),
				message.getCreatedAt(),
				sources,
				rewrittenQuestion
		);
	}

	private ChatMessageSourceResponse toSourceResponse(ChatMessageSource source) {
		return new ChatMessageSourceResponse(
				source.getDocumentId(),
				source.getChunkId(),
				source.getPageNumber(),
				source.getSimilarityScore()
		);
	}

	private ChatSessionResponse toSessionResponse(ChatSession session) {
		// Response session chứa selectedDocumentIds để FE nhận diện chat single-document khi mở lại.
		return new ChatSessionResponse(
				session.getSessionId(),
				session.getTitle(),
				session.getChatMode(),
				session.getFolderId(),
				session.getKnowledgePolicy(),
				session.getModel(),
				session.getTemperature(),
				chatSessionDocumentRepository.findDocumentIdsBySessionId(session.getSessionId()),
				session.getCreatedAt(),
				session.getUpdatedAt()
		);
	}

	private ChatSession findOwnedSession(Long sessionId) {
		var userId = currentUserService.getCurrentUserId();
		// Điều kiện userId và isDeletedFalse ngăn user đọc hoặc gửi vào session của người khác/đã xóa.
		return chatSessionRepository.findBySessionIdAndUserIdAndIsDeletedFalse(sessionId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
	}

	private ChatMode resolveMode(String mode) {
		return switch (mode) {
			case "SelectedDocuments" -> ChatMode.SELECTED_DOCUMENTS;
			case "UserStorage" -> ChatMode.USER_STORAGE;
			default -> throw new IllegalArgumentException("Unsupported chat mode");
		};
	}

	private KnowledgePolicy resolvePolicy(ChatMode mode, Boolean useGeneralKnowledge) {
		if (mode == ChatMode.SELECTED_DOCUMENTS) {
			// SelectedDocuments luôn giới hạn trong danh sách document đã chọn.
			return KnowledgePolicy.DOCUMENTS_ONLY;
		}
		// Giá trị request được ưu tiên. Nếu client bỏ trống, dùng cấu hình mặc định
		// rag.user-storage.allow-general-knowledge (hiện tại là false).
		var includePublicDocuments = useGeneralKnowledge != null
				? useGeneralKnowledge
				: ragProperties.getUserStorage().isAllowGeneralKnowledge();
		return includePublicDocuments
				? KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
				: KnowledgePolicy.DOCUMENTS_ONLY;
	}

	private String normalizeTitle(String title) {
		return StringUtils.hasText(title) ? title.trim() : DEFAULT_TITLE;
	}

	private void touchSession(ChatSession session) {
		session.setUpdatedAt(Instant.now());
		chatSessionRepository.save(session);
	}

	private String buildContext(List<RetrievedChunk> chunks) {
		var context = new StringBuilder();
		// Ghép nội dung chunk kèm document/chunk index để prompt và citation giữ được nguồn truy xuất.
		for (var retrieved : chunks) {
			var chunk = retrieved.getChunk();
			context.append("[Document ")
					.append(chunk.getDocument().getDocumentId())
					.append(", chunk ")
					.append(chunk.getChunkIndex())
					.append("]\n")
					.append(chunk.getContent())
					.append("\n\n");
		}
		return context.toString().trim();
	}

	private String noContextMessage(ChatMode mode, KnowledgePolicy policy) {
		return switch (mode) {
			case SELECTED_DOCUMENTS ->
					"I cannot find this information in the documents you selected.";
			case USER_STORAGE -> policy == KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
					? "I cannot find sufficient information in your documents or public documents to answer this question."
					: "I cannot find sufficient information in your documents to answer this question.";
		};
	}

	private double roundScore(double score) {
		return Math.round(score * 10000.0) / 10000.0;
	}
}
