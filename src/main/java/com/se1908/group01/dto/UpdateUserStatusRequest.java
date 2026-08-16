package com.se1908.group01.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateUserStatusRequest(
		@NotNull(message = "Status is required")
		@Pattern(
				regexp = "ACTIVE|BLOCKED",
				message = "Status must be ACTIVE or BLOCKED"
		)
		String status
) {
}
