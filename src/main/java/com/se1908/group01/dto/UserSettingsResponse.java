package com.se1908.group01.dto;

import com.se1908.group01.enums.Theme;
import com.se1908.group01.enums.Visibility;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {

	private Theme theme;
	private Visibility profileVisibility;
	private Visibility activityVisibility;
	private boolean allowFriendRequests;
	private boolean showOnlineStatus;
	private Instant updatedAt;
}
