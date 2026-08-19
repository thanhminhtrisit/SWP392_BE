package com.se1908.group01.service;

import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.dto.AdminDocumentShareApprovalResponse;
import com.se1908.group01.dto.DocumentPageResponse;
import com.se1908.group01.dto.DocumentShareLinkResponse;
import com.se1908.group01.dto.DocumentShareResponse;
import com.se1908.group01.dto.FileAccessUrlResponse;
import com.se1908.group01.enums.ShareApprovalStatus;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

	/**
	 * Tạo metadata tài liệu và schedule ingestion cho file mới upload.
	 * Được DocumentController gọi qua POST /api/documents/upload.
	 */
	DocumentUploadResponse upload(MultipartFile file, Boolean isPublic, Long folderId) throws IOException;

	DocumentUploadResponse moveToTrash(Long documentId);

	List<DocumentUploadResponse> getMyDocuments();

	List<DocumentUploadResponse> getStarredDocuments();

	DocumentUploadResponse getDocumentDetail(Long documentId);

	DocumentUploadResponse renameDocument(Long documentId, String originalFileName);

	DocumentUploadResponse moveDocumentToFolder(Long documentId, Long folderId);

	FileAccessUrlResponse getPreviewUrl(Long documentId);

	FileAccessUrlResponse getDownloadUrl(Long documentId);

	List<DocumentUploadResponse> getPublicDocuments();

	DocumentUploadResponse getPublicDocumentDetail(Long documentId);

	FileAccessUrlResponse getPublicPreviewUrl(Long documentId);

	FileAccessUrlResponse getPublicDownloadUrl(Long documentId);

	/**
	 * Lưu một tài liệu công khai từ Community về kho tài liệu cá nhân (My Files).
	 *
	 * @param documentId ID của tài liệu công khai trên Community
	 * @param folderId   ID của thư mục đích trong My Files (tùy chọn, có thể null)
	 * @return thông tin chi tiết của tài liệu mới được tạo trong My Files
	 */
	DocumentUploadResponse savePublicDocumentToMyFiles(Long documentId, Long folderId);

	DocumentShareLinkResponse createShareLink(Long documentId);

	DocumentShareLinkResponse disableShareLink(Long documentId);

	DocumentUploadResponse getDocumentByShareLink(String token);

	FileAccessUrlResponse getShareLinkPreviewUrl(String token);

	FileAccessUrlResponse getShareLinkDownloadUrl(String token);

	DocumentShareResponse saveShareLinkToSharedWithMe(String token);

	DocumentShareResponse shareDocumentWithUser(Long documentId, String email);

	List<DocumentShareResponse> getDocumentShares(Long documentId);

	void removeUserShare(Long documentId, Long userId);

	List<DocumentUploadResponse> getSharedWithMeDocuments();

	DocumentUploadResponse getSharedWithMeDocumentDetail(Long documentId);

	FileAccessUrlResponse getSharedWithMePreviewUrl(Long documentId);

	FileAccessUrlResponse getSharedWithMeDownloadUrl(Long documentId);

	void removeSharedWithMeDocument(Long documentId);

	void bulkRemoveSharedWithMeDocuments(List<Long> documentIds);

	void bulkMoveDocuments(List<Long> documentIds, Long folderId);

	void bulkMoveToTrash(List<Long> documentIds);

	/**
	 * Lưu một tài liệu được chia sẻ trực tiếp với tôi về kho tài liệu cá nhân (My Files).
	 *
	 * @param documentId ID của tài liệu được chia sẻ
	 * @param folderId   ID của thư mục đích trong My Files (tùy chọn, có thể null)
	 * @return thông tin chi tiết của tài liệu mới được tạo trong My Files
	 */
	DocumentUploadResponse saveSharedWithMeDocumentToMyFiles(Long documentId, Long folderId);

	DocumentUploadResponse updateVisibility(Long documentId, Boolean isPublic);

	DocumentUploadResponse reviewShareApproval(Long documentId, ShareApprovalStatus status);

	Page<AdminDocumentShareApprovalResponse> getDocumentShareApprovals(
			ShareApprovalStatus status,
			com.se1908.group01.enums.DocumentShareApprovalType shareType,
			Pageable pageable
	);

	DocumentUploadResponse updateStarred(Long documentId, Boolean isStarred);

	List<DocumentUploadResponse> getTrash();

	DocumentUploadResponse restoreFromTrash(Long documentId);

	void deletePermanently(Long documentId);

	DocumentPageResponse filterMyDocuments(
			List<Long> tagIds,
			String contentType,
			Instant createdFrom,
			Instant createdTo,
			String sort,
			int page,
			int size
	);
}
