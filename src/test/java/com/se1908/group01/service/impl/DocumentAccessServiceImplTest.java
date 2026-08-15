package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentFolder;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.repository.DocumentFolderRepository;
import com.se1908.group01.repository.DocumentRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentAccessServiceImplTest {

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private DocumentFolderRepository documentFolderRepository;

	private DocumentAccessServiceImpl documentAccessService;

	@BeforeEach
	void setUp() {
		documentAccessService = new DocumentAccessServiceImpl(documentRepository, documentFolderRepository);
	}

	@Test
	void getAllReadyDocumentsForUserUsesOwnedScopeWhenPublicDocumentsAreDisabled() {
		var userId = 1L;
		var documents = List.of(document(10L));
		when(documentRepository.findOwnedDocumentsByStatus(userId, DocumentStatus.READY))
				.thenReturn(documents);

		var result = documentAccessService.getAllReadyDocumentsForUser(userId, null, false);

		assertEquals(documents, result);
		verify(documentRepository).findOwnedDocumentsByStatus(userId, DocumentStatus.READY);
	}

	@Test
	void getAllReadyDocumentsForUserUsesAccessibleScopeWhenPublicDocumentsAreEnabled() {
		var userId = 1L;
		var documents = List.of(document(10L), document(20L));
		when(documentRepository.findAllAccessibleDocumentsByStatus(userId, DocumentStatus.READY))
				.thenReturn(documents);

		var result = documentAccessService.getAllReadyDocumentsForUser(userId, null, true);

		assertEquals(documents, result);
		verify(documentRepository).findAllAccessibleDocumentsByStatus(userId, DocumentStatus.READY);
	}

	@Test
	void getAllReadyDocumentsForUserUsesOwnedFolderScopeWhenPublicDocumentsAreDisabled() {
		var userId = 1L;
		var folderId = 5L;
		var folder = new DocumentFolder();
		var documents = List.of(document(10L));
		when(documentFolderRepository.findByFolderIdAndUserId(folderId, userId))
				.thenReturn(java.util.Optional.of(folder));
		when(documentRepository.findOwnedDocumentsByFolderAndStatus(
				userId,
				folderId,
				DocumentStatus.READY
		)).thenReturn(documents);

		var result = documentAccessService.getAllReadyDocumentsForUser(userId, folderId, false);

		assertEquals(documents, result);
		verify(documentRepository).findOwnedDocumentsByFolderAndStatus(
				userId,
				folderId,
				DocumentStatus.READY
		);
	}

	@Test
	void getReadyDocumentsForChatRejectsPartiallyResolvedSelection() {
		var userId = 1L;
		var requestedIds = List.of(10L, 20L);
		when(documentRepository.findAccessibleDocumentsByIdsAndStatus(
				requestedIds,
				userId,
				DocumentStatus.READY
		)).thenReturn(List.of(document(10L)));

		assertThrows(
				IllegalArgumentException.class,
				() -> documentAccessService.getReadyDocumentsForChat(userId, requestedIds)
		);
	}

	private Document document(Long documentId) {
		var document = new Document();
		document.setDocumentId(documentId);
		return document;
	}
}
