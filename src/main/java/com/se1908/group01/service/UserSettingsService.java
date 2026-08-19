package com.se1908.group01.service;

import com.se1908.group01.dto.UpdateUserSettingsRequest;
import com.se1908.group01.dto.UserSettingsResponse;

public interface UserSettingsService {

	UserSettingsResponse getMySettings();

	UserSettingsResponse updateMySettings(UpdateUserSettingsRequest request);
}
