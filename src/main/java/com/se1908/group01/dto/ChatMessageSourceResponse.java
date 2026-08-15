package com.se1908.group01.dto;

public record ChatMessageSourceResponse(
		Long documentId,
		Long chunkId,
		Integer pageNumber,
		Double score
) {
}
