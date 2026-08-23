package com.se1908.group01.service.impl;

import com.se1908.group01.dto.ChunkData;
import com.se1908.group01.dto.TextSegment;
import com.se1908.group01.entity.Document;
// [SUA NGAY 2026-08-22 - co ho tro cua AI] Sua import entity -> enums (loi co san, lam vo testCompile).
import com.se1908.group01.enums.DocumentStatus;
import com.se1908.group01.exception.DoclingUnavailableException;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.DoclingService;
import com.se1908.group01.service.DocumentChunkingService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.DocumentParsingService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceImplTest {

	@Mock
	private DocumentParsingService parsingService;

	@Mock
	private DocumentChunkingService chunkingService;

	@Mock
	private DoclingService doclingService;

	@Mock
	private DocumentEmbeddingService embeddingService;

	@Mock
	private DocumentChunkRepository documentChunkRepository;

	@Mock
	private DocumentRepository documentRepository;

	private DocumentIngestionServiceImpl service;
	private Document document;
	private MockMultipartFile file;

	@BeforeEach
	void setUp() {
		service = new DocumentIngestionServiceImpl(
				parsingService,
				chunkingService,
				doclingService,
				embeddingService,
				documentChunkRepository,
				documentRepository
		);
		document = new Document();
		document.setDocumentId(10L);
		file = new MockMultipartFile(
				"file",
				"sample.docx",
				"application/vnd.openxmlformats-officedocument"
						+ ".wordprocessingml.document",
				"test content".getBytes(StandardCharsets.UTF_8)
		);
	}

	@Test
	void ingestUsesDoclingChunksWhenSupported() throws Exception {
		var chunks = List.of(
				new ChunkData(0, 1, "Docling chunk")
		);
		when(doclingService.supports(file)).thenReturn(true);
		when(doclingService.chunk(file)).thenReturn(chunks);
		when(embeddingService.embedVectors(
				List.of("Docling chunk")
		)).thenReturn(List.of("[0.1,0.2]"));

		int count = service.ingest(document, file);

		assertEquals(1, count);
		assertEquals(DocumentStatus.READY, document.getStatus());
		verify(parsingService, never()).extractSegments(file, document);
		verify(chunkingService, never()).chunk(anyList());
		verify(documentChunkRepository).saveAll(anyList());
	}

	@Test
	void ingestFallsBackWhenDoclingIsUnavailable() throws Exception {
		var segments = List.of(
				new TextSegment("Legacy content", 2)
		);
		var chunks = List.of(
				new ChunkData(0, 2, "Legacy chunk")
		);
		when(doclingService.supports(file)).thenReturn(true);
		when(doclingService.chunk(file)).thenThrow(
				new DoclingUnavailableException(
						"Docling is unavailable"
				)
		);
		when(doclingService.isFallbackEnabled()).thenReturn(true);
		when(parsingService.extractSegments(file, document))
				.thenReturn(segments);
		when(chunkingService.chunk(segments)).thenReturn(chunks);
		when(embeddingService.embedVectors(
				List.of("Legacy chunk")
		)).thenReturn(List.of("[0.3,0.4]"));

		int count = service.ingest(document, file);

		assertEquals(1, count);
		assertEquals(DocumentStatus.READY, document.getStatus());
		verify(parsingService).extractSegments(file, document);
		verify(chunkingService).chunk(segments);
		verify(documentChunkRepository).saveAll(anyList());
	}

	@Test
	void ingestFailsWhenDoclingUnavailableAndFallbackDisabled() throws Exception {
		when(doclingService.supports(file)).thenReturn(true);
		when(doclingService.chunk(file)).thenThrow(
				new DoclingUnavailableException("Docling is unavailable")
		);
		when(doclingService.isFallbackEnabled()).thenReturn(false);

		assertThrows(
				DoclingUnavailableException.class,
				() -> service.ingest(document, file)
		);

		verify(parsingService, never()).extractSegments(file, document);
		verify(embeddingService, never()).embedVectors(anyList());
	}

	@Test
	void ingestFailsWhenNoTextCanBeExtracted() throws Exception {
		when(doclingService.supports(file)).thenReturn(false);
		when(parsingService.extractSegments(file, document)).thenReturn(List.of());
		when(chunkingService.chunk(List.of())).thenReturn(List.of());

		assertThrows(
				IllegalStateException.class,
				() -> service.ingest(document, file)
		);

		assertEquals(DocumentStatus.FAILED, document.getStatus());
		verify(embeddingService, never()).embedVectors(anyList());
	}
}
