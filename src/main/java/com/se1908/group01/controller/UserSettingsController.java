package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.UpdateUserSettingsRequest;
import com.se1908.group01.dto.UserSettingsResponse;
import com.se1908.group01.service.UserSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/settings")
@RequiredArgsConstructor
public class UserSettingsController {

	private final UserSettingsService userSettingsService;

	@GetMapping
	public ApiResponse<UserSettingsResponse> getMySettings() {
		var response = userSettingsService.getMySettings();
		return ApiResponse.success("Get settings successfully", response);
	}

	@PatchMapping
	public ApiResponse<UserSettingsResponse> updateMySettings(@Valid @RequestBody UpdateUserSettingsRequest request) {
		var response = userSettingsService.updateMySettings(request);
		return ApiResponse.success("Update settings successfully", response);
	}
}
