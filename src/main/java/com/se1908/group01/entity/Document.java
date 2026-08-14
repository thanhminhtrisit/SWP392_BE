package com.se1908.group01.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
		name = "document",
		indexes = {
				@Index(name = "idx_document_user_deleted_uploaded", columnList = "user_id, is_deleted, uploaded_at"),
				@Index(name = "idx_document_content_type", columnList = "content_type")
		}
)
/**
 * Đại diện JPA cho metadata và trạng thái xử lý của file đã upload.
 * Nội dung binary nằm trong Azure Blob Storage; bảng này lưu owner, object key, kích thước, visibility và lifecycle state.
 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Sửa mô tả "S3" -> "Azure Blob Storage" cho khớp thực tế
 * sau khi chuyển nhà cung cấp lưu trữ. Cột `s3_key` và trường `s3Key` GIỮ NGUYÊN TÊN có chủ ý:
 * blob name của Azure cũng chỉ là một chuỗi định danh nên nhét vào vừa khít, còn đổi tên cột
 * sẽ kéo theo migration + entity + DTO — không đáng.
 */
public class Document {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "document_id")
	private Long documentId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "folder_id")
	private Long folderId;

	@Column(name = "original_file_name", nullable = false, length = 512)
	private String originalFileName;

	@Column(name = "s3_key", length = 1024)
	private String s3Key;

	@Column(name = "content_type", length = 255)
	private String contentType;

	@Column(name = "file_size")
	private Long fileSize;

	@Column(name = "is_public", nullable = false)
	private Boolean isPublic = Boolean.FALSE;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = Boolean.FALSE;

	@Column(name = "is_starred", nullable = false)
	private Boolean isStarred = Boolean.FALSE;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private DocumentStatus status = DocumentStatus.UPLOADED;

	@Column(name = "uploaded_at", nullable = false)
	private Instant uploadedAt;

	@PrePersist
	void prePersist() {
		// Gán giá trị mặc định để mọi upload mới đều có timestamp và lifecycle state xác định.
		if (uploadedAt == null) {
			uploadedAt = Instant.now();
		}
		if (status == null) {
			status = DocumentStatus.UPLOADED;
		}
		if (isDeleted == null) {
			isDeleted = Boolean.FALSE;
		}
		if (isStarred == null) {
			isStarred = Boolean.FALSE;
		}
	}

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

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public void setStatus(DocumentStatus status) {
		this.status = status;
	}

	public Instant getUploadedAt() {
		return uploadedAt;
	}

	public void setUploadedAt(Instant uploadedAt) {
		this.uploadedAt = uploadedAt;
	}
}
