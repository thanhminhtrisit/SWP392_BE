package com.se1908.group01.controller;

import com.se1908.group01.dto.AdminUserListResponse;
import com.se1908.group01.dto.AdminUserResponse;
import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.UpdateUserStatusRequest;
import com.se1908.group01.service.AdminUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

	private final AdminUserService adminUserService;

	public AdminUserController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@GetMapping
	public ApiResponse<AdminUserListResponse> getUsers(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String role,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(
				"Get users successfully",
				adminUserService.getUsers(keyword, status, role, page, size)
		);
	}

	@GetMapping("/{userId}")
	public ApiResponse<AdminUserResponse> getUser(@PathVariable Long userId) {
		return ApiResponse.success(
				"Get user detail successfully",
				adminUserService.getUser(userId)
		);
	}

	@PatchMapping("/{userId}/status")
	public ApiResponse<AdminUserResponse> updateStatus(
			@PathVariable Long userId,
			@Valid @RequestBody UpdateUserStatusRequest request
	) {
		return ApiResponse.success(
				"Update user status successfully",
				adminUserService.updateStatus(userId, request.status())
		);
	}
}
