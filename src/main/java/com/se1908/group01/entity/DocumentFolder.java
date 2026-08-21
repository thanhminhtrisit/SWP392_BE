package com.se1908.group01.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
		name = "document_folder",
		uniqueConstraints = @UniqueConstraint(name = "uk_document_folder_user_name", columnNames = {"user_id", "name"})
)
/**
 * Metadata folder thuộc user, được document.folder_id tham chiếu sau upload.
 */
public class DocumentFolder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "folder_id")
	private Long folderId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	// [SUA NGAY 2026-08-20 - co ho tro cua AI] Them columnDefinition NVARCHAR(100).
	// Ten thu muc do nguoi dung dat -> phai luu duoc tieng Viet. Migration V3 doi cot;
	// cot nay vuong rang buoc UNIQUE uk_document_folder_user_name nen migration phai
	// drop -> alter -> tao lai.
	@Column(name = "name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
	private String name;

	@Column(name = "is_starred", nullable = false)
	private Boolean isStarred = Boolean.FALSE;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = Boolean.FALSE;

	@Column(name = "deleted_at")
	private Instant deletedAt;

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
		if (isStarred == null) {
			isStarred = Boolean.FALSE;
		}
		if (isDeleted == null) {
			isDeleted = Boolean.FALSE;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public Long getFolderId() {
		return folderId;
	}

	public void setFolderId(Long folderId) {
		this.folderId = folderId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getIsStarred() {
		return isStarred;
	}

	public void setIsStarred(Boolean isStarred) {
		this.isStarred = isStarred;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
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
