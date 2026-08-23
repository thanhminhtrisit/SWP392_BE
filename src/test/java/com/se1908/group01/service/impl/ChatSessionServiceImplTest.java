package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.se1908.group01.config.RagProperties;
import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.dto.CreateChatSessionRequest;
import com.se1908.group01.dto.SendChatMessageRequest;
import com.se1908.group01.entity.ChatMessage;
import com.se1908.group01.entity.ChatSession;
import com.se1908.group01.entity.Document;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import com.se1908.group01.enums.SupportedAiModel;
import com.se1908.group01.repository.ChatMessageRepository;
import com.se1908.group01.repository.ChatMessageSourceRepository;
import com.se1908.group01.repository.ChatSessionDocumentRepository;
import com.se1908.group01.repository.ChatSessionRepository;
import com.se1908.group01.service.AiGenerationOptionsService;
import com.se1908.group01.service.ChatConversationMemoryService;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.LlmClient;
import com.se1908.group01.service.PromptBuilderService;
import com.se1908.group01.service.QueryRewriteService;
import com.se1908.group01.service.SubscriptionEntitlementService;
import com.se1908.group01.service.VectorSearchService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceImplTest {

	@Mock
	private CurrentUserService currentUserService;
	@Mock
	private DocumentAccessService documentAccessService;
	@Mock
	private DocumentEmbeddingService documentEmbeddingService;
	@Mock
	private VectorSearchService vectorSearchService;
	@Mock
	private PromptBuilderService promptBuilderService;
	@Mock
	private LlmClient llmClient;
	@Mock
	private AiGenerationOptionsService aiGenerationOptionsService;
	@Mock
	private ChatConversationMemoryService chatConversationMemoryService;
	// [SUA NGAY 2026-08-22 - co ho tro cua AI] Mock moi. Constructor cua
	// ChatSessionServiceImpl da them mot tham so nen test cu khong compile duoc nua.
	@Mock
	private QueryRewriteService queryRewriteService;
	@Mock
	private ChatSessionRepository chatSessionRepository;
	@Mock
	private ChatSessionDocumentRepository chatSessionDocumentRepository;
	@Mock
	private ChatMessageRepository chatMessageRepository;
	@Mock
	private ChatMessageSourceRepository chatMessageSourceRepository;
	@Mock
	private SubscriptionEntitlementService subscriptionEntitlementService;

	private ChatSessionServiceImpl chatSessionService;

	@BeforeEach
	void setUp() {
		chatSessionService = new ChatSessionServiceImpl(
				currentUserService,
				documentAccessService,
				documentEmbeddingService,
				vectorSearchService,
				promptBuilderService,
				llmClient,
				aiGenerationOptionsService,
				chatConversationMemoryService,
				queryRewriteService,
				chatSessionRepository,
				chatSessionDocumentRepository,
				chatMessageRepository,
				chatMessageSourceRepository,
				new RagProperties(),
				subscriptionEntitlementService
		);
	}

	@Test
	void createSelectedSessionPersistsResolvedDocumentsAndGenerationOptions() {
		var document = new Document();
		document.setDocumentId(10L);
		var request = new CreateChatSessionRequest(
				"Study session",
				"SelectedDocuments",
				List.of(10L),
				null,
				true,
				"gpt-5.6-terra",
				0.3
		);
		when(currentUserService.getCurrentUserId()).thenReturn(1L);
		when(documentAccessService.getReadyDocumentsForChat(1L, List.of(10L)))
				.thenReturn(List.of(document));
		when(aiGenerationOptionsService.resolve("gpt-5.6-terra", 0.3))
				.thenReturn(new AiGenerationOptions(SupportedAiModel.GPT_5_6_TERRA, 0.3));
		when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
			ChatSession session = invocation.getArgument(0);
			session.setSessionId(5L);
			return session;
		});
		when(chatSessionDocumentRepository.findDocumentIdsBySessionId(5L))
				.thenReturn(List.of(10L));

		var response = chatSessionService.createSession(request);

		assertEquals(5L, response.sessionId());
		assertEquals(ChatMode.SELECTED_DOCUMENTS, response.mode());
		assertEquals(KnowledgePolicy.DOCUMENTS_ONLY, response.policy());
		assertEquals("gpt-5.6-terra", response.model());
		assertEquals(0.3, response.temperature());
		assertEquals(List.of(10L), response.selectedDocumentIds());
		verify(chatSessionDocumentRepository).save(any());
	}

	@Test
	void sendMessageRejectsUnsupportedModelBeforeSavingMessage() {
		var session = new ChatSession();
		session.setSessionId(5L);
		session.setUserId(1L);
		session.setModel("gpt-5.6-luna");
		session.setTemperature(0.3);
		var request = new SendChatMessageRequest(
				"Explain this",
				"unsupported-model",
				null,
				null
		);

		when(currentUserService.getCurrentUserId()).thenReturn(1L);
		when(chatSessionRepository.findBySessionIdAndUserIdAndIsDeletedFalse(5L, 1L))
				.thenReturn(Optional.of(session));
		when(aiGenerationOptionsService.resolve("unsupported-model", 0.3))
				.thenThrow(new IllegalArgumentException("Unsupported AI model"));

		assertThrows(
				IllegalArgumentException.class,
				() -> chatSessionService.sendMessage(5L, request)
		);

		verifyNoInteractions(chatMessageRepository);
	}

	@Test
	void sendMessageUsesAndPersistsRequestedModel() {
		var session = new ChatSession();
		session.setSessionId(5L);
		session.setUserId(1L);
		session.setChatMode(ChatMode.USER_STORAGE);
		session.setKnowledgePolicy(KnowledgePolicy.DOCUMENTS_ONLY);
		session.setModel("gpt-5.6-luna");
		session.setTemperature(0.3);
		var request = new SendChatMessageRequest(
				"Explain this",
				"gpt-5.6-terra",
				true,
				0.7
		);
		var options = new AiGenerationOptions(SupportedAiModel.GPT_5_6_TERRA, 0.7);

		when(currentUserService.getCurrentUserId()).thenReturn(1L);
		when(chatSessionRepository.findBySessionIdAndUserIdAndIsDeletedFalse(5L, 1L))
				.thenReturn(Optional.of(session));
		when(aiGenerationOptionsService.resolve("gpt-5.6-terra", 0.7))
				.thenReturn(options);
		when(chatConversationMemoryService.getRecentMessages(5L)).thenReturn(List.of());
		when(documentAccessService.getAllReadyDocumentsForUser(1L, null, true))
				.thenReturn(List.of());
		when(chatMessageRepository.save(any(ChatMessage.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		chatSessionService.sendMessage(5L, request);

		assertEquals("gpt-5.6-terra", session.getModel());
		assertEquals(KnowledgePolicy.DOCUMENTS_PLUS_GENERAL, session.getKnowledgePolicy());
		assertEquals(0.7, session.getTemperature());
		verify(chatSessionRepository, atLeastOnce()).save(session);
	}
}
