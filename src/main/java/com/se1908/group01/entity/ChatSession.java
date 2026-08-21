package com.se1908.group01.entity;

import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
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
import java.time.Instant;

@Entity
@Table(name = "chat_session")
/** Lưu cấu hình, phạm vi document và trạng thái soft-delete của một cuộc trò chuyện. */
public class ChatSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "session_id")
	private Long sessionId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	// [SUA NGAY 2026-08-20 - co ho tro cua AI] Them columnDefinition NVARCHAR(200).
	// Tieu de phien do nguoi dung dat, truoc la varchar nen mat dau: "On tap SWD392" luu
	// thanh "On t?p SWD392". Migration V3 doi cot tuong ung.
	@Column(name = "title", nullable = false, length = 200, columnDefinition = "NVARCHAR(200)")
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(name = "chat_mode", nullable = false, length = 30)
	private ChatMode chatMode;

	@Column(name = "folder_id")
	private Long folderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "knowledge_policy", nullable = false, length = 40)
	private KnowledgePolicy knowledgePolicy;

	@Column(name = "model", nullable = false, length = 100)
	private String model;

	@Column(name = "temperature", nullable = false)
	private Double temperature;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = Boolean.FALSE;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		var now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (isDeleted == null) {
			isDeleted = Boolean.FALSE;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public Long getSessionId() {
		return sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ChatMode getChatMode() {
		return chatMode;
	}

	public void setChatMode(ChatMode chatMode) {
		this.chatMode = chatMode;
	}

	public Long getFolderId() {
		return folderId;
	}

	public void setFolderId(Long folderId) {
		this.folderId = folderId;
	}

	public KnowledgePolicy getKnowledgePolicy() {
		return knowledgePolicy;
	}

	public void setKnowledgePolicy(KnowledgePolicy knowledgePolicy) {
		this.knowledgePolicy = knowledgePolicy;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
