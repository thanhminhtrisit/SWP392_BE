package com.se1908.group01.dto;

import java.util.List;

public record ChatMessageListResponse(
		List<ChatMessageResponse> messages,
		int page,
		int size,
		long totalElements,
		int totalPages
) {
}
