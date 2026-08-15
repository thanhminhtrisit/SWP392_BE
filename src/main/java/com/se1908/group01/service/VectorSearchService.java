package com.se1908.group01.service;

import com.se1908.group01.dto.RetrievedChunk;
import java.util.List;
import org.springframework.lang.Nullable;

public interface VectorSearchService {

	List<RetrievedChunk> search(Long documentId, String queryEmbeddingVector, int limit);

	List<RetrievedChunk> search(
			String queryEmbeddingVector,
			@Nullable List<Long> documentIds,
			@Nullable Long userId,
			@Nullable Long folderId,
			int limit
	);
}
