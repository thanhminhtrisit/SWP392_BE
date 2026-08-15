package com.se1908.group01.service.impl;

import com.se1908.group01.dto.ChunkData;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentChunk;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.exception.DoclingUnavailableException;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.DocumentChunkingService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.DocumentIngestionService;
import com.se1908.group01.service.DocumentParsingService;
import com.se1908.group01.service.DoclingService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
/**
 * Chuyển tài liệu đã upload thành các vector chunk có thể tìm kiếm.
 * Các stage runtime: PARSING -> tạo chunk -> INDEXING/embedding -> lưu document_chunk -> READY.
 */
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

	private static final Logger log = LoggerFactory.getLogger(DocumentIngestionServiceImpl.class);
	private final DocumentParsingService parsingService;
	private final DocumentChunkingService chunkingService;
	private final DoclingService doclingService;
	private final DocumentEmbeddingService embeddingService;
	private final DocumentChunkRepository documentChunkRepository;
	private final DocumentRepository documentRepository;

	public DocumentIngestionServiceImpl(
			DocumentParsingService parsingService,
			DocumentChunkingService chunkingService,
			DoclingService doclingService,
			DocumentEmbeddingService embeddingService,
			DocumentChunkRepository documentChunkRepository,
			DocumentRepository documentRepository
	) {
		this.parsingService = parsingService;
		this.chunkingService = chunkingService;
		this.doclingService = doclingService;
		this.embeddingService = embeddingService;
		this.documentChunkRepository = documentChunkRepository;
		this.documentRepository = documentRepository;
	}

	@Override
	/**
	 * Tạo và lưu toàn bộ representation có thể tìm kiếm của một tài liệu.
	 * Được DocumentIngestionJobServiceImpl gọi sau khi transaction upload đã commit.
	 */
	public int ingest(Document document, MultipartFile file) throws IOException {
		if (document == null || document.getDocumentId() == null) {
			throw new IllegalArgumentException("Document is required");
		}

		// Đánh dấu document đang parse trước khi extract text từ bytes đã upload.
		updateStatus(document, DocumentStatus.PARSING);
		var chunks = createChunks(document, file);

		// Replace existing chunks for this document (if any).
		documentChunkRepository.deleteByDocumentDocumentId(document.getDocumentId());

		if (chunks.isEmpty()) {
			updateStatus(document, DocumentStatus.FAILED);
			throw new IllegalStateException("No text content could be extracted from document");
		}

		// Chỉ embedding sau khi extract text thành công; vector cần cho semantic search/chat.
		updateStatus(document, DocumentStatus.INDEXING);
		var texts = chunks.stream().map(ChunkData::getContent).toList();
		var vectors = embeddingService.embedVectors(texts);
		if (vectors.size() != chunks.size()) {
			throw new IllegalStateException("Embedding vectors size mismatch");
		}

		List<DocumentChunk> entities = new ArrayList<>(chunks.size());
		// Gắn mỗi text chunk với document, page metadata và embedding vector đã serialize.
		for (int i = 0; i < chunks.size(); i++) {
			var c = chunks.get(i);
			var e = new DocumentChunk();
			e.setDocument(document);
			e.setChunkIndex(c.getChunkIndex());
			e.setPageNumber(c.getPageNumber());
			e.setContent(c.getContent());
			e.setEmbeddingVector(vectors.get(i));
			entities.add(e);
		}

		// Lưu chunk và vector cùng nhau để READY có nghĩa document đã có nội dung được index.
		documentChunkRepository.saveAll(entities);
		updateStatus(document, DocumentStatus.READY);
		return entities.size();
	}

	private List<ChunkData> createChunks(
			Document document,
			MultipartFile file
	) throws IOException {
		if (doclingService.supports(file)) {
			try {
				// Ưu tiên Docling cho format được hỗ trợ vì nó giữ nội dung tài liệu có cấu trúc.
				var chunks = doclingService.chunk(file);
				log.info(
						"Docling created {} chunks for documentId={}",
						chunks.size(),
						document.getDocumentId()
				);
				return chunks;
			} catch (DoclingUnavailableException exception) {
				if (!doclingService.isFallbackEnabled()) {
					// Nếu không bật fallback, file type này bắt buộc phải dùng được Docling.
					throw exception;
				}
				log.warn(
						"Docling unavailable for documentId={}. "
								+ "Falling back to legacy parser. reason={}",
						document.getDocumentId(),
						exception.getMessage()
				);
				log.debug(
						"Docling fallback details for documentId={}",
						document.getDocumentId(),
						exception
				);
			}
		}

		// Dùng parser/chunker cũ cho format không hỗ trợ hoặc khi Docling được phép fallback.
		var segments = parsingService.extractSegments(file, document);
		return chunkingService.chunk(segments);
	}

	private void updateStatus(Document document, DocumentStatus status) {
		document.setStatus(status);
		documentRepository.save(document);
	}
}
