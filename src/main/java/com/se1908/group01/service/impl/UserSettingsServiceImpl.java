package com.se1908.group01.service.impl;

import com.se1908.group01.dto.UpdateUserSettingsRequest;
import com.se1908.group01.dto.UserSettingsResponse;
import com.se1908.group01.entity.UserSettings;
import com.se1908.group01.enums.Theme;
import com.se1908.group01.enums.Visibility;
import com.se1908.group01.repository.UserSettingsRepository;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

	private final UserSettingsRepository userSettingsRepository;
	private final CurrentUserService currentUserService;

	@Override
	@Transactional
	public UserSettingsResponse getMySettings() {
		var userId = currentUserService.getCurrentUserId();
		var settings = userSettingsRepository.findByUserId(userId)
				.orElseGet(() -> createDefaultSettings(userId));
		return toResponse(settings);
	}

	@Override
	@Transactional
	public UserSettingsResponse updateMySettings(UpdateUserSettingsRequest request) {
		var userId = currentUserService.getCurrentUserId();
		var settings = userSettingsRepository.findByUserId(userId)
				.orElseGet(() -> createDefaultSettings(userId));

		if (request.getTheme() != null) {
			settings.setTheme(request.getTheme());
		}
		if (request.getProfileVisibility() != null) {
			settings.setProfileVisibility(request.getProfileVisibility());
		}
		if (request.getActivityVisibility() != null) {
			settings.setActivityVisibility(request.getActivityVisibility());
		}
		if (request.getAllowFriendRequests() != null) {
			settings.setAllowFriendRequests(request.getAllowFriendRequests());
		}
		if (request.getShowOnlineStatus() != null) {
			settings.setShowOnlineStatus(request.getShowOnlineStatus());
		}

		return toResponse(userSettingsRepository.save(settings));
	}

	private UserSettings createDefaultSettings(Long userId) {
		var defaults = UserSettings.builder()
				.userId(userId)
				.theme(Theme.SYSTEM)
				.profileVisibility(Visibility.PUBLIC)
				.activityVisibility(Visibility.PUBLIC)
				.allowFriendRequests(true)
				.showOnlineStatus(true)
				.build();
		return userSettingsRepository.save(defaults);
	}

	private UserSettingsResponse toResponse(UserSettings settings) {
		return UserSettingsResponse.builder()
				.theme(settings.getTheme())
				.profileVisibility(settings.getProfileVisibility())
				.activityVisibility(settings.getActivityVisibility())
				.allowFriendRequests(settings.isAllowFriendRequests())
				.showOnlineStatus(settings.isShowOnlineStatus())
				.updatedAt(settings.getUpdatedAt())
				.build();
	}
}
