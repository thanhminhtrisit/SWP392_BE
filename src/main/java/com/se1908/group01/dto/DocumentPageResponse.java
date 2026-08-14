package com.se1908.group01.dto;

import java.util.List;

public record DocumentPageResponse(
		List<DocumentUploadResponse> documents,
		int page,
		int size,
		long totalElements,
		int totalPages
) {
}
