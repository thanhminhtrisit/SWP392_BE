package com.se1908.group01.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.entity.DocumentChunk;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.service.VectorSearchService;
import java.util.Comparator;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
/**
 * Tìm các document chunk gần câu hỏi nhất bằng cosine similarity.
 * Vector được lưu dưới dạng JSON text trong document_chunk và được tính toán trong Java.
 */
public class VectorSearchServiceImpl implements VectorSearchService {

	private final DocumentChunkRepository documentChunkRepository;
	private final ObjectMapper objectMapper;

	public VectorSearchServiceImpl(DocumentChunkRepository documentChunkRepository, ObjectMapper objectMapper) {
		this.documentChunkRepository = documentChunkRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<RetrievedChunk> search(Long documentId, String queryEmbeddingVector, int limit) {
		// Nhánh single-document chỉ đọc chunk thuộc đúng document đã được access check ở service phía trên.
		if (documentId == null) {
			throw new IllegalArgumentException("Document ID is required");
		}
		if (!StringUtils.hasText(queryEmbeddingVector)) {
			throw new IllegalArgumentException("Query embedding vector is required");
		}

		var queryVector = parseVector(queryEmbeddingVector);
		// Repository lấy toàn bộ chunk của document; sau đó service xếp hạng semantic trong bộ nhớ.
		return documentChunkRepository.findByDocumentDocumentIdOrderByChunkIndexAsc(documentId)
				.stream()
				// Bỏ chunk không có embedding hoặc khác chiều vector để tránh đưa context không hợp lệ vào LLM.
				.map(chunk -> score(chunk, queryVector))
				.filter(result -> result != null)
				.sorted(Comparator.comparingDouble(RetrievedChunk::getScore).reversed())
				.limit(Math.max(1, limit))
				.toList();
	}

	@Override
	public List<RetrievedChunk> search(
			String queryEmbeddingVector,
			@Nullable List<Long> documentIds,
			@Nullable Long userId,
			@Nullable Long folderId,
			int limit
	) {
		if (!StringUtils.hasText(queryEmbeddingVector)) {
			throw new IllegalArgumentException("Query embedding vector is required");
		}
		if ((documentIds == null || documentIds.isEmpty()) && userId == null) {
			throw new IllegalArgumentException("Either documentIds or userId must be provided");
		}

		var queryVector = parseVector(queryEmbeddingVector);
		List<DocumentChunk> chunks;

		if (documentIds != null && !documentIds.isEmpty()) {
			// Danh sách ID đã được DocumentAccessService kiểm tra quyền/READY trước khi đi vào đây.
			chunks = documentChunkRepository.findByDocumentIds(documentIds);
		} else if (folderId != null) {
			// Nhánh fallback theo folder tự áp điều kiện accessible và READY tại repository.
			chunks = documentChunkRepository.findChunksByUserAndFolderAccessible(userId, folderId, DocumentStatus.READY);
		} else {
			// Không có document/folder cụ thể thì tìm trên toàn bộ storage mà user được phép đọc.
			chunks = documentChunkRepository.findChunksByUserAccessible(userId, DocumentStatus.READY);
		}

		// Đây là xếp hạng in-memory: DB chỉ nạp chunk, Java tính cosine rồi lấy TOP_K toàn cục.
		// Hiện chưa có minimum similarity threshold nên mọi vector hợp lệ đều có thể lọt vào kết quả.
		return chunks.stream()
				.map(chunk -> score(chunk, queryVector))
				.filter(result -> result != null)
				.sorted(Comparator.comparingDouble(RetrievedChunk::getScore).reversed())
				.limit(Math.max(1, limit))
				.toList();
	}

	private RetrievedChunk score(DocumentChunk chunk, double[] queryVector) {
		if (!StringUtils.hasText(chunk.getEmbeddingVector())) {
			// Chunk thiếu embedding không thể tham gia semantic search.
			return null;
		}
		var chunkVector = parseVector(chunk.getEmbeddingVector());
		if (chunkVector.length != queryVector.length) {
			// Khác số chiều thường cho thấy chunk và question được tạo bởi model embedding khác nhau.
			return null;
		}
		return new RetrievedChunk(chunk, cosineSimilarity(queryVector, chunkVector));
	}

	private double[] parseVector(String json) {
		try {
			// embedding_vector được lưu trong SQL Server dưới dạng JSON text, không phải native vector type.
			return objectMapper.readValue(json, double[].class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to parse embedding vector", e);
		}
	}

	private double cosineSimilarity(double[] left, double[] right) {
		// cosine = tích vô hướng / (độ lớn vector trái * độ lớn vector phải).
		double dot = 0.0;
		double leftMagnitude = 0.0;
		double rightMagnitude = 0.0;
		for (int i = 0; i < left.length; i++) {
			dot += left[i] * right[i];
			leftMagnitude += left[i] * left[i];
			rightMagnitude += right[i] * right[i];
		}
		if (leftMagnitude == 0.0 || rightMagnitude == 0.0) {
			// Tránh chia cho 0; zero-vector được xem là không tương đồng.
			return 0.0;
		}
		return dot / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
	}
}
