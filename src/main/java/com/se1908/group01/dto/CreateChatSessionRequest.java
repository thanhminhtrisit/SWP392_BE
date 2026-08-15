package com.se1908.group01.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request tạo session; single-document dùng mode SelectedDocuments và selectedDocumentIds gồm một phần tử. */
public record CreateChatSessionRequest(
		@Size(max = 200, message = "Title must not exceed 200 characters")
		String title,
		@NotNull(message = "Mode is required")
		@Pattern(
				regexp = "SelectedDocuments|UserStorage",
				message = "mode must be 'SelectedDocuments' or 'UserStorage'"
		)
		String mode,
		List<Long> selectedDocumentIds,
		Long folderId,
		Boolean useGeneralKnowledge,
		String model,
		@DecimalMin(value = "0.0", message = "Temperature must be between 0.0 and 1.0")
		@DecimalMax(value = "1.0", message = "Temperature must be between 0.0 and 1.0")
		Double temperature
) {
}
