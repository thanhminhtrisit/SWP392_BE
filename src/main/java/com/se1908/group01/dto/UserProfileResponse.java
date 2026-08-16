package com.se1908.group01.dto;

import com.se1908.group01.enums.AccountStatus;
import com.se1908.group01.enums.Role;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

	private Long userId;
	private String fullName;
	private String email;
	private String bio;
	private String avatarUrl;
	private Role role;
	private AccountStatus status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
