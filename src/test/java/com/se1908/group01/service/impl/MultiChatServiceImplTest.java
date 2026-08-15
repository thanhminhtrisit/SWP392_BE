package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentChunk;
import com.se1908.group01.enums.SupportedAiModel;
import com.se1908.group01.service.AiGenerationOptionsService;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.LlmClient;
import com.se1908.group01.service.PromptBuilderService;
import com.se1908.group01.service.SubscriptionEntitlementService;
import com.se1908.group01.service.VectorSearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MultiChatServiceImplTest {

	@Mock
	private DocumentAccessService documentAccessService;

	@Mock
	private CurrentUserService currentUserService;

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
	private SubscriptionEntitlementService subscriptionEntitlementService;

	private MultiChatServiceImpl multiChatService;

	@BeforeEach
	void setUp() {
		multiChatService = new MultiChatServiceImpl(
				documentAccessService,
				currentUserService,
				documentEmbeddingService,
				vectorSearchService,
				promptBuilderService,
				llmClient,
				aiGenerationOptionsService,
				subscriptionEntitlementService
		);
		when(currentUserService.getCurrentUserId()).thenReturn(1L);
		when(aiGenerationOptionsService.resolve(null, null))
				.thenReturn(new AiGenerationOptions(SupportedAiModel.GPT_5_6_LUNA, 0.2));
	}

	@Test
	void userStorageSearchesOwnedDocumentsOnlyWhenNoFolderProvided() {
		var request = request("UserStorage", null);
		when(documentAccessService.getAllReadyDocumentsForUser(1L, null, false))
				.thenReturn(List.of());

		var response = multiChatService.askMulti(request);

		assertEquals("USER_STORAGE", response.getMode());
		assertEquals("gpt-5.6-luna", response.getModel());
		assertEquals(0.2, response.getTemperature());
		assertEquals(
				"I cannot find sufficient information in your documents to answer this question.",
				response.getAnswer()
		);
		verify(documentAccessService).getAllReadyDocumentsForUser(1L, null, false);
	}

	@Test
	void userStorageScopesToFolderWhenFolderIdProvided() {
		var request = request("UserStorage", 42L);
		when(documentAccessService.getAllReadyDocumentsForUser(1L, 42L, false))
				.thenReturn(List.of());

		var response = multiChatService.askMulti(request);

		assertEquals(
				"I cannot find sufficient information in this folder to answer this question.",
				response.getAnswer()
		);
		verify(documentAccessService).getAllReadyDocumentsForUser(1L, 42L, false);
	}

	@Test
	void selectedDocumentsResolvesScopeBySelectedIdsOnly() {
		var request = request("SelectedDocuments", null);
		request.setSelectedDocumentIds(List.of(10L, 20L));
		when(documentAccessService.getReadyDocumentsForChat(1L, request.getSelectedDocumentIds()))
				.thenReturn(List.of());

		var response = multiChatService.askMulti(request);

		assertEquals("SELECTED_DOCUMENTS", response.getMode());
		assertEquals(
				"I cannot find this information in the documents you selected.",
				response.getAnswer()
		);
		verify(documentAccessService).getReadyDocumentsForChat(1L, List.of(10L, 20L));
	}

	@Test
	void populatesSourcesFromRetrievedChunksOnSuccessfulAnswer() {
		var request = request("SelectedDocuments", null);
		request.setSelectedDocumentIds(List.of(10L));

		var document = new Document();
		document.setDocumentId(10L);
		document.setOriginalFileName("SRS.pdf");
		when(documentAccessService.getReadyDocumentsForChat(1L, List.of(10L)))
				.thenReturn(List.of(document));

		when(documentEmbeddingService.embedQuestion("What is this about?")).thenReturn("[0.1]");

		var chunk = new DocumentChunk();
		chunk.setChunkId(15L);
		chunk.setChunkIndex(2);
		chunk.setDocument(document);
		chunk.setContent("The AI Study Hub system is a centralized web platform...");
		var retrievedChunk = new RetrievedChunk(chunk, 0.87);
		when(vectorSearchService.search("[0.1]", List.of(10L), null, null, 10))
				.thenReturn(List.of(retrievedChunk));

		when(promptBuilderService.buildMultiDocumentQuestionPrompt(
				com.se1908.group01.enums.ChatMode.SELECTED_DOCUMENTS,
				"[Document 10, chunk 2]\nThe AI Study Hub system is a centralized web platform...",
				"What is this about?"
		)).thenReturn("prompt");
		when(llmClient.generateAnswer("prompt", new AiGenerationOptions(SupportedAiModel.GPT_5_6_LUNA, 0.2)))
				.thenReturn("It is a study platform.");

		var response = multiChatService.askMulti(request);

		assertEquals(List.of(10L), response.getUsedDocumentIds());
		assertEquals(1, response.getSources().size());
		var source = response.getSources().getFirst();
		assertEquals(10L, source.documentId());
		assertEquals("SRS.pdf", source.documentName());
		assertEquals(15L, source.chunkId());
		assertEquals("The AI Study Hub system is a centralized web platform...", source.contentPreview());
		assertEquals(0.87, source.similarityScore());
	}

	private MultiChatAskRequest request(String mode, Long folderId) {
		var request = new MultiChatAskRequest();
		request.setMode(mode);
		request.setQuestion("What is this about?");
		request.setFolderId(folderId);
		return request;
	}
}
