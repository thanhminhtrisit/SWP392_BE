package com.se1908.group01.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/** Request chứa câu hỏi mới gửi vào một persistent chat session. */
public record SendChatMessageRequest(
		@NotBlank(message = "Question is required")
		String question,
		String model,
		Boolean useGeneralKnowledge,
		@DecimalMin(value = "0.0", message = "Temperature must be between 0.0 and 1.0")
		@DecimalMax(value = "1.0", message = "Temperature must be between 0.0 and 1.0")
		Double temperature
) {
}
