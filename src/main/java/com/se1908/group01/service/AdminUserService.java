package com.se1908.group01.service;

import com.se1908.group01.dto.AdminUserListResponse;
import com.se1908.group01.dto.AdminUserResponse;

public interface AdminUserService {

	AdminUserListResponse getUsers(
			String keyword,
			String status,
			String role,
			int page,
			int size
	);

	AdminUserResponse getUser(Long userId);

	AdminUserResponse updateStatus(Long userId, String status);
}
