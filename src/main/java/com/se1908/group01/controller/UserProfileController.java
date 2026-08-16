package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.ChangePasswordRequest;
import com.se1908.group01.dto.UpdateUserProfileRequest;
import com.se1908.group01.dto.UserProfileResponse;
import com.se1908.group01.exception.FileStorageException;
import com.se1908.group01.service.UserProfileService;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

	private final UserProfileService userProfileService;

	@GetMapping
	public ApiResponse<UserProfileResponse> getMyProfile() {
		var response = userProfileService.getMyProfile();
		return ApiResponse.success("Get profile successfully", response);
	}

	@PatchMapping
	public ApiResponse<UserProfileResponse> updateMyProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
		var response = userProfileService.updateMyProfile(request);
		return ApiResponse.success("Update profile successfully", response);
	}

	@PatchMapping("/password")
	public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		userProfileService.changePassword(request);
		return ApiResponse.success("Change password successfully", null);
	}

	@PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<UserProfileResponse> updateAvatar(@RequestParam("file") MultipartFile file) {
		try {
			var response = userProfileService.updateAvatar(file);
			return ApiResponse.success("Update avatar successfully", response);
		// [SUA NGAY 2026-08-11 - co ho tro cua AI] Doi S3Exception -> FileStorageException.
		// LY DO: bo phu thuoc vao kieu rieng cua AWS SDK o tang controller sau khi chuyen
		// phan luu tru sang Azure Blob. Ma trang thai 503 giu nguyen.
		} catch (FileStorageException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "File storage upload failed", ex);
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File read failed", ex);
		}
	}
}
