package com.se1908.group01.dto;

public record MultiChatSourceResponse(
		Long documentId,
		String documentName,
		Long chunkId,
		String contentPreview,
		Double similarityScore
) {
}
