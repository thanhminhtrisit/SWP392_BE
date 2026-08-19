package com.se1908.group01.dto;

import com.se1908.group01.enums.DocumentStatus;
import com.se1908.group01.enums.ShareApprovalStatus;
import java.time.Instant;

/**
 * Metadata trả về ngay sau upload; status ingestion có thể đổi từ UPLOADED sang READY hoặc FAILED.
 */
public class DocumentUploadResponse {

	private Long documentId;
	private Long userId;
	private String ownerEmail;
	private Long folderId;
	private String originalFileName;
	private String s3Key;
	private String contentType;
	private Long fileSize;
	private Boolean isPublic;
	private Boolean isDeleted;
	private Boolean isStarred;
	private DocumentStatus status;
	private ShareApprovalStatus shareApprovalStatus;
	private Instant uploadedAt;
	private Instant deletedAt;

	public Long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getOwnerEmail() {
		return ownerEmail;
	}

	public void setOwnerEmail(String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	public Long getFolderId() {
		return folderId;
	}

	public void setFolderId(Long folderId) {
		this.folderId = folderId;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	public String getS3Key() {
		return s3Key;
	}

	public void setS3Key(String s3Key) {
		this.s3Key = s3Key;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public Boolean getIsStarred() {
		return isStarred;
	}

	public void setIsStarred(Boolean isStarred) {
		this.isStarred = isStarred;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public void setStatus(DocumentStatus status) {
		this.status = status;
	}

	public ShareApprovalStatus getShareApprovalStatus() {
		return shareApprovalStatus;
	}

	public void setShareApprovalStatus(ShareApprovalStatus shareApprovalStatus) {
		this.shareApprovalStatus = shareApprovalStatus;
	}

	public Instant getUploadedAt() {
		return uploadedAt;
	}

	public void setUploadedAt(Instant uploadedAt) {
		this.uploadedAt = uploadedAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}
}
