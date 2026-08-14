package com.se1908.group01.entity;

import com.se1908.group01.enums.Theme;
import com.se1908.group01.enums.Visibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
		name = "user_settings",
		uniqueConstraints = @UniqueConstraint(name = "uk_user_settings_user_id", columnNames = "user_id")
)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserSettings {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "setting_id")
	private Long settingId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private Theme theme;

	@Column(name = "profile_visibility", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private Visibility profileVisibility;

	@Column(name = "activity_visibility", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private Visibility activityVisibility;

	@Column(name = "allow_friend_requests", nullable = false)
	private boolean allowFriendRequests;

	@Column(name = "show_online_status", nullable = false)
	private boolean showOnlineStatus;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	@PreUpdate
	void stampUpdatedAt() {
		updatedAt = Instant.now();
	}
}
