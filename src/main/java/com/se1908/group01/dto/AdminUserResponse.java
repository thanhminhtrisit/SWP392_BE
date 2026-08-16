package com.se1908.group01.dto;

import com.se1908.group01.enums.AccountStatus;
import com.se1908.group01.enums.AuthProvider;
import com.se1908.group01.enums.Role;
import java.time.LocalDateTime;

public record AdminUserResponse(
		Long userId,
		String fullName,
		String email,
		AuthProvider provider,
		Role role,
		AccountStatus status,
		boolean verified,
		String bio,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
