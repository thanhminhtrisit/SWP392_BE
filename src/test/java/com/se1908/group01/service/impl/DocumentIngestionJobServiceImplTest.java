package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.entity.Document;
// [SUA NGAY 2026-08-22 - co ho tro cua AI] Sua import entity -> enums (loi co san, lam vo testCompile).
import com.se1908.group01.enums.DocumentStatus;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.DocumentIngestionService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionJobServiceImplTest {

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private DocumentIngestionService documentIngestionService;

	@TempDir
	Path tempDir;

	@Test
	void failedIngestionMarksDocumentFailedAndDeletesTempFile() throws Exception {
		var document = new Document();
		document.setDocumentId(10L);
		document.setStatus(DocumentStatus.UPLOADED);
		var tempFile = Files.writeString(tempDir.resolve("upload.tmp"), "content");

		when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
		when(documentIngestionService.ingest(any(), any()))
				.thenThrow(new IllegalStateException("No content"));

		var service = new DocumentIngestionJobServiceImpl(
				documentRepository,
				documentIngestionService
		);
		service.ingestAsync(10L, tempFile, "sample.pdf", "application/pdf");

		verify(documentRepository).save(document);
		assertFalse(Files.exists(tempFile));
		org.junit.jupiter.api.Assertions.assertEquals(DocumentStatus.FAILED, document.getStatus());
	}
}
