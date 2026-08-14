package com.se1908.group01.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
		name = "ai_token_usage",
		indexes = {
				@Index(name = "idx_ai_token_usage_user_created", columnList = "user_id, created_at")
		}
)
public class AiTokenUsage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "usage_id")
	private Long usageId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "estimated_tokens", nullable = false)
	private Long estimatedTokens;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getUsageId() {
		return usageId;
	}

	public void setUsageId(Long usageId) {
		this.usageId = usageId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getEstimatedTokens() {
		return estimatedTokens;
	}

	public void setEstimatedTokens(Long estimatedTokens) {
		this.estimatedTokens = estimatedTokens;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
