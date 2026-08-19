package com.se1908.group01.entity;

import com.se1908.group01.enums.DocumentShareApprovalType;
import com.se1908.group01.enums.ShareApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
		name = "document_share_approvals",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_document_share_approval_type",
						columnNames = {"document_id", "share_type"}
				)
		}
)
public class DocumentShareApproval {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "document_share_approval_id")
	private Long documentShareApprovalId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	@Enumerated(EnumType.STRING)
	@Column(name = "share_type", nullable = false, length = 32)
	private DocumentShareApprovalType shareType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private ShareApprovalStatus status = ShareApprovalStatus.UNREVIEWED;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (updatedAt == null) {
			updatedAt = createdAt;
		}
		if (status == null) {
			status = ShareApprovalStatus.UNREVIEWED;
		}
	}

	public Long getDocumentShareApprovalId() {
		return documentShareApprovalId;
	}

	public void setDocumentShareApprovalId(Long documentShareApprovalId) {
		this.documentShareApprovalId = documentShareApprovalId;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public DocumentShareApprovalType getShareType() {
		return shareType;
	}

	public void setShareType(DocumentShareApprovalType shareType) {
		this.shareType = shareType;
	}

	public ShareApprovalStatus getStatus() {
		return status;
	}

	public void setStatus(ShareApprovalStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
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
