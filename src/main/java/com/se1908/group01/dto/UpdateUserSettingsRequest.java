package com.se1908.group01.dto;

import com.se1908.group01.enums.Theme;
import com.se1908.group01.enums.Visibility;
import lombok.Data;

@Data
public class UpdateUserSettingsRequest {

	private Theme theme;
	private Visibility profileVisibility;
	private Visibility activityVisibility;
	private Boolean allowFriendRequests;
	private Boolean showOnlineStatus;
}
