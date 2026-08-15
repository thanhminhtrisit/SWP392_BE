package com.se1908.group01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChatSessionRequest(
		@NotBlank(message = "Title is required")
		@Size(max = 200, message = "Title must not exceed 200 characters")
		String title,
		Boolean useGeneralKnowledge
) {
}
