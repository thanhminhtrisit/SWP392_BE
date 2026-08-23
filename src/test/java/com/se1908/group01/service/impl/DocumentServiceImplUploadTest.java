package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.config.AzureStorageProperties;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentFolder;
// [SUA NGAY 2026-08-22 - co ho tro cua AI] Sua import entity -> enums (loi co san, lam vo testCompile).
import com.se1908.group01.enums.DocumentStatus;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.repository.ChatSessionDocumentRepository;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.repository.DocumentFolderRepository;
import com.se1908.group01.repository.DocumentRepository;
// [SUA NGAY 2026-08-22 - co ho tro cua AI] Import moi cho mock con thieu.
import com.se1908.group01.repository.DocumentShareApprovalRepository;
import com.se1908.group01.repository.DocumentShareLinkRepository;
import com.se1908.group01.repository.DocumentShareRepository;
import com.se1908.group01.repository.DocumentTagRepository;
import com.se1908.group01.repository.FriendshipRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentIngestionJobService;
import com.se1908.group01.service.DocumentIngestionService;
import com.se1908.group01.service.FileValidationService;
import com.se1908.group01.service.FileStorageService;
import com.se1908.group01.service.SubscriptionEntitlementService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplUploadTest {

	@Mock private FileValidationService fileValidationService;
	/*
	 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Doi mock S3StorageService -> FileStorageService
	 * va S3Properties -> AzureStorageProperties sau khi chuyen luu tru sang Azure Blob.
	 *
	 * LY DO: bat buoc, khong phai lam dep - hai kieu cu da bi xoa nen test khong compile.
	 * Va `spring-boot:run` co chay test-compile, test khong compile la khong chay duoc app.
	 *
	 * KHONG DOI NOI DUNG KIEM THU: cac test nay chi kiem tra DocumentServiceImpl goi dung
	 * thu tu (upload truoc, luu database sau, upload loi thi khong cham database, luu
	 * database loi thi xoa object da upload). Logic do khong phu thuoc nha cung cap storage.
	 */
	@Mock private FileStorageService fileStorageService;
	@Mock private AzureStorageProperties azureStorageProperties;
	@Mock private DocumentRepository documentRepository;
	@Mock private DocumentFolderRepository documentFolderRepository;
	@Mock private DocumentChunkRepository documentChunkRepository;
	@Mock private DocumentTagRepository documentTagRepository;
	@Mock private DocumentIngestionService documentIngestionService;
	// [SUA NGAY 2026-08-22 - co ho tro cua AI] Mock con THIEU. DocumentServiceImpl da nhan
	// them DocumentShareApprovalRepository o vi tri thu 9 nhung test khong duoc cap nhat theo,
	// nen testCompile vo. Loi co san, khong lien quan den query rewriting.
	@Mock private DocumentShareApprovalRepository documentShareApprovalRepository;
	@Mock private DocumentShareLinkRepository documentShareLinkRepository;
	@Mock private DocumentShareRepository documentShareRepository;
	@Mock private FriendshipRepository friendshipRepository;
	@Mock private UserRepository userRepository;
	@Mock private DocumentIngestionJobService documentIngestionJobService;
	@Mock private CurrentUserService currentUserService;
	@Mock private ChatSessionDocumentRepository chatSessionDocumentRepository;
	@Mock private SubscriptionEntitlementService subscriptionEntitlementService;

	private DocumentServiceImpl service;
	private MockMultipartFile file;
	private SubscriptionPlan plan;

	@BeforeEach
	void setUp() {
		service = new DocumentServiceImpl(
				fileValidationService,
				fileStorageService,
				azureStorageProperties,
				documentRepository,
				documentFolderRepository,
				documentChunkRepository,
				documentTagRepository,
				documentIngestionService,
				// [SUA NGAY 2026-08-22 - co ho tro cua AI] Bo sung tham so thu 9 con thieu.
				documentShareApprovalRepository,
				documentShareLinkRepository,
				documentShareRepository,
				friendshipRepository,
				userRepository,
				documentIngestionJobService,
				currentUserService,
				chatSessionDocumentRepository,
				subscriptionEntitlementService
		);
		file = new MockMultipartFile(
				"file",
				"..\\unsafe\r\nname.pdf",
				"application/pdf",
				"PDF content".getBytes(StandardCharsets.UTF_8)
		);
		plan = SubscriptionPlan.builder()
				.maxUploadSizeMb(35)
				.build();
	}

	@Test
	void uploadStoresMetadataAndSchedulesIngestion() throws Exception {
		var tempFile = Path.of("document-ingestion-test.tmp");
		when(currentUserService.getCurrentUserId()).thenReturn(7L);
		when(subscriptionEntitlementService.getActivePlan(7L)).thenReturn(plan);
		when(documentFolderRepository.findByFolderIdAndUserId(19L, 7L))
				.thenReturn(Optional.of(new DocumentFolder()));
		when(azureStorageProperties.getKeyPrefix()).thenReturn("study");
		when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
			Document document = invocation.getArgument(0);
			document.setDocumentId(11L);
			return document;
		});
		when(documentIngestionJobService.copyToTempFile(file)).thenReturn(tempFile);

		var response = service.upload(file, true, 19L);

		assertEquals(11L, response.getDocumentId());
		assertEquals(7L, response.getUserId());
		assertEquals(19L, response.getFolderId());
		assertEquals("unsafe name.pdf", response.getOriginalFileName());
		assertEquals(true, response.getIsPublic());
		assertEquals(DocumentStatus.UPLOADED, response.getStatus());

		var keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(fileStorageService).uploadPrivate(any(), keyCaptor.capture());
		assertTrue(keyCaptor.getValue().startsWith("study/documents/7/"));
		assertTrue(keyCaptor.getValue().endsWith("-unsafe name.pdf"));
		verify(fileValidationService).validateForUpload(file, 35);
		verify(subscriptionEntitlementService).enforceUploadEntitlements(7L, file, plan, false);
		verify(documentIngestionJobService).ingestAsync(
				11L,
				tempFile,
				"unsafe name.pdf",
				"application/pdf"
		);
	}

	@Test
	void uploadDeletesS3ObjectWhenMetadataSaveFails() throws Exception {
		when(currentUserService.getCurrentUserId()).thenReturn(7L);
		when(subscriptionEntitlementService.getActivePlan(7L)).thenReturn(plan);
		when(documentRepository.save(any(Document.class)))
				.thenThrow(new IllegalStateException("Database unavailable"));

		assertThrows(IllegalStateException.class, () -> service.upload(file, false, null));

		var keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(fileStorageService).uploadPrivate(any(), keyCaptor.capture());
		verify(fileStorageService).delete(keyCaptor.getValue());
		verify(documentIngestionJobService, never()).ingestAsync(any(), any(), anyString(), anyString());
	}

	@Test
	void uploadStopsBeforeDatabaseWhenS3UploadFails() throws Exception {
		when(currentUserService.getCurrentUserId()).thenReturn(7L);
		when(subscriptionEntitlementService.getActivePlan(7L)).thenReturn(plan);
		doThrow(new IOException("S3 unavailable"))
				.when(fileStorageService)
				.uploadPrivate(any(), anyString());

		assertThrows(IOException.class, () -> service.upload(file, false, null));

		verify(fileValidationService).validateForUpload(eq(file), eq(35));
		verify(documentRepository, never()).save(any(Document.class));
		verify(documentIngestionJobService, never()).copyToTempFile(any());
	}
}
