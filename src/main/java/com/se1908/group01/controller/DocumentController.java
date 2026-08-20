package com.se1908.group01.controller;

import com.se1908.group01.dto.*;
import com.se1908.group01.enums.ShareApprovalStatus;
import com.se1908.group01.enums.DocumentShareApprovalType;
import com.se1908.group01.exception.FileStorageException;
import com.se1908.group01.service.DocumentService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/documents")
/**
 * Cung cấp các API tài liệu được dashboard sử dụng, bao gồm upload multipart.
 * Luồng runtime: React UploadModal -> POST /api/documents/upload -> DocumentService.
 */
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	@GetMapping("/filter")
	public ApiResponse<DocumentPageResponse> filterMyDocuments(
			@RequestParam(required = false) List<Long> tagIds,
			@RequestParam(required = false) String contentType,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
			@RequestParam(defaultValue = "NEWEST") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		var response = documentService.filterMyDocuments(tagIds, contentType, createdFrom, createdTo, sort, page, size);
		return ApiResponse.success("Filter documents successfully", response);
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	/**
	 * Nhận file multipart và giao toàn bộ việc validation, persistence cho service layer.
	 * Response chứa metadata tài liệu; parsing và embedding tiếp tục bất đồng bộ sau khi transaction commit.
	 */
	public ApiResponse<DocumentUploadResponse> upload(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "isPublic", required = false) Boolean isPublic,
			@RequestParam(value = "folderId", required = false) Long folderId
	) {
		try {
			var response = documentService.upload(file, isPublic, folderId);
			return ApiResponse.success("Upload document successfully", response);
		// [SUA NGAY 2026-08-11 - co ho tro cua AI] Doi S3Exception -> FileStorageException.
		// LY DO: kieu rieng cua AWS SDK khong nen ro ri len tang controller. Bat exception
		// chung cua tang storage thi lan sau doi nha cung cap se khong phai sua file nay.
		// Ma trang thai 503 giu nguyen: frontend dang dua vao no de bao "storage tam loi".
		} catch (FileStorageException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "File storage upload failed", ex);
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File read failed", ex);
		}
	}

	@GetMapping("/my")
	public ApiResponse<List<DocumentUploadResponse>> getMyDocuments() {
		var response = documentService.getMyDocuments();
		return ApiResponse.success("Get my documents successfully", response);
	}

	@GetMapping("/starred")
	public ApiResponse<List<DocumentUploadResponse>> getStarredDocuments() {
		var response = documentService.getStarredDocuments();
		return ApiResponse.success("Get starred documents successfully", response);
	}

	@GetMapping("/share-link/{token}")
	public ApiResponse<DocumentUploadResponse> getDocumentByShareLink(@PathVariable String token) {
		var response = documentService.getDocumentByShareLink(token);
		return ApiResponse.success("Get shared document successfully", response);
	}

	@GetMapping("/share-link/{token}/preview-url")
	public ApiResponse<FileAccessUrlResponse> getShareLinkPreviewUrl(@PathVariable String token) {
		var response = documentService.getShareLinkPreviewUrl(token);
		return ApiResponse.success("Get shared document preview URL successfully", response);
	}

	@GetMapping("/share-link/{token}/download-url")
	public ApiResponse<FileAccessUrlResponse> getShareLinkDownloadUrl(@PathVariable String token) {
		var response = documentService.getShareLinkDownloadUrl(token);
		return ApiResponse.success("Get shared document download URL successfully", response);
	}

	@PostMapping("/share-link/{token}/save")
	public ApiResponse<DocumentShareResponse> saveShareLinkToSharedWithMe(@PathVariable String token) {
		var response = documentService.saveShareLinkToSharedWithMe(token);
		return ApiResponse.success("Save shared document successfully", response);
	}

	@GetMapping("/shared-with-me")
	public ApiResponse<List<DocumentUploadResponse>> getSharedWithMeDocuments() {
		var response = documentService.getSharedWithMeDocuments();
		return ApiResponse.success("Get documents shared with me successfully", response);
	}

	@GetMapping("/shared-with-me/{documentId}")
	public ApiResponse<DocumentUploadResponse> getSharedWithMeDocumentDetail(@PathVariable Long documentId) {
		var response = documentService.getSharedWithMeDocumentDetail(documentId);
		return ApiResponse.success("Get shared document detail successfully", response);
	}

	@GetMapping("/shared-with-me/{documentId}/preview-url")
	public ApiResponse<FileAccessUrlResponse> getSharedWithMePreviewUrl(@PathVariable Long documentId) {
		var response = documentService.getSharedWithMePreviewUrl(documentId);
		return ApiResponse.success("Get shared document preview URL successfully", response);
	}

	@GetMapping("/shared-with-me/{documentId}/download-url")
	public ApiResponse<FileAccessUrlResponse> getSharedWithMeDownloadUrl(@PathVariable Long documentId) {
		var response = documentService.getSharedWithMeDownloadUrl(documentId);
		return ApiResponse.success("Get shared document download URL successfully", response);
	}

	@DeleteMapping("/shared-with-me/{documentId}")
	public ApiResponse<Void> removeSharedWithMeDocument(@PathVariable Long documentId) {
		documentService.removeSharedWithMeDocument(documentId);
		return ApiResponse.success("Remove shared document successfully", null);
	}

	@PostMapping("/shared-with-me/bulk-remove")
	public ApiResponse<Void> bulkRemoveSharedWithMeDocuments(@RequestBody List<Long> documentIds) {
		documentService.bulkRemoveSharedWithMeDocuments(documentIds);
		return ApiResponse.success("Bulk remove shared documents successfully", null);
	}

	@PatchMapping("/bulk-move")
	public ApiResponse<Void> bulkMoveDocuments(@RequestBody com.se1908.group01.dto.BulkMoveDocumentRequest request) {
		documentService.bulkMoveDocuments(request.getDocumentIds(), request.getFolderId());
		return ApiResponse.success("Bulk move documents successfully", null);
	}

	@PostMapping("/bulk-trash")
	public ApiResponse<Void> bulkMoveToTrash(@RequestBody List<Long> documentIds) {
		documentService.bulkMoveToTrash(documentIds);
		return ApiResponse.success("Bulk move documents to trash successfully", null);
	}

	@GetMapping("/{documentId}")
	public ApiResponse<DocumentUploadResponse> getDocumentDetail(@PathVariable Long documentId) {
		var response = documentService.getDocumentDetail(documentId);
		return ApiResponse.success("Get document detail successfully", response);
	}

	@PatchMapping("/{documentId}/rename")
	public ApiResponse<DocumentUploadResponse> renameDocument(
			@PathVariable Long documentId,
			@Valid @RequestBody DocumentRenameRequest request
	) {
		var response = documentService.renameDocument(documentId, request.getOriginalFileName());
		return ApiResponse.success("Rename document successfully", response);
	}

	@PatchMapping("/{documentId}/folder")
	/**
	 * Áp dụng folder tùy chọn mà UploadModal yêu cầu sau khi upload trả về document id.
	 */
	public ApiResponse<DocumentUploadResponse> moveDocumentToFolder(
			@PathVariable Long documentId,
			@RequestBody DocumentMoveFolderRequest request
	) {
		var response = documentService.moveDocumentToFolder(documentId, request.getFolderId());
		return ApiResponse.success("Move document to folder successfully", response);
	}

	@GetMapping("/{documentId}/preview-url")
	public ApiResponse<FileAccessUrlResponse> getPreviewUrl(@PathVariable Long documentId) {
		var response = documentService.getPreviewUrl(documentId);
		return ApiResponse.success("Get document preview URL successfully", response);
	}

	@GetMapping("/{documentId}/download-url")
	public ApiResponse<FileAccessUrlResponse> getDownloadUrl(@PathVariable Long documentId) {
		var response = documentService.getDownloadUrl(documentId);
		return ApiResponse.success("Get document download URL successfully", response);
	}

	@PostMapping("/{documentId}/share-link")
	public ApiResponse<DocumentShareLinkResponse> createShareLink(@PathVariable Long documentId) {
		var response = documentService.createShareLink(documentId);
		return ApiResponse.success("Create document share link successfully", response);
	}

	@DeleteMapping("/{documentId}/share-link")
	public ApiResponse<DocumentShareLinkResponse> disableShareLink(@PathVariable Long documentId) {
		var response = documentService.disableShareLink(documentId);
		return ApiResponse.success("Disable document share link successfully", response);
	}

	@GetMapping("/{documentId}/shares/users")
	public ApiResponse<List<DocumentShareResponse>> getDocumentShares(@PathVariable Long documentId) {
		var response = documentService.getDocumentShares(documentId);
		return ApiResponse.success("Get document shares successfully", response);
	}

	@PostMapping("/{documentId}/shares/users")
	public ApiResponse<DocumentShareResponse> shareDocumentWithUser(
			@PathVariable Long documentId,
			@Valid @RequestBody ShareDocumentWithUserRequest request
	) {
		var response = documentService.shareDocumentWithUser(documentId, request.getEmail());
		return ApiResponse.success("Share document with user successfully", response);
	}

	@DeleteMapping("/{documentId}/shares/users/{userId}")
	public ApiResponse<Void> removeUserShare(
			@PathVariable Long documentId,
			@PathVariable Long userId
	) {
		documentService.removeUserShare(documentId, userId);
		return ApiResponse.success("Remove document share successfully", null);
	}

	@GetMapping("/public")
	public ApiResponse<List<DocumentUploadResponse>> getPublicDocuments() {
		var response = documentService.getPublicDocuments();
		return ApiResponse.success("Get public documents successfully", response);
	}

	@GetMapping("/public/{documentId}")
	public ApiResponse<DocumentUploadResponse> getPublicDocumentDetail(@PathVariable Long documentId) {
		var response = documentService.getPublicDocumentDetail(documentId);
		return ApiResponse.success("Get public document detail successfully", response);
	}

	@GetMapping("/public/{documentId}/preview-url")
	public ApiResponse<FileAccessUrlResponse> getPublicPreviewUrl(@PathVariable Long documentId) {
		var response = documentService.getPublicPreviewUrl(documentId);
		return ApiResponse.success("Get public document preview URL successfully", response);
	}

	@GetMapping("/public/{documentId}/download-url")
	public ApiResponse<FileAccessUrlResponse> getPublicDownloadUrl(@PathVariable Long documentId) {
		var response = documentService.getPublicDownloadUrl(documentId);
		return ApiResponse.success("Get public document download URL successfully", response);
	}

	@PostMapping("/public/{documentId}/save-to-my-files")
	public ApiResponse<DocumentUploadResponse> savePublicDocumentToMyFiles(
			@PathVariable Long documentId,
			@RequestParam(value = "folderId", required = false) Long folderId
	) {
		var response = documentService.savePublicDocumentToMyFiles(documentId, folderId);
		return ApiResponse.success("Save public document to My Files successfully", response);
	}

	@PostMapping("/shared-with-me/{documentId}/save-to-my-files")
	public ApiResponse<DocumentUploadResponse> saveSharedWithMeDocumentToMyFiles(
			@PathVariable Long documentId,
			@RequestParam(value = "folderId", required = false) Long folderId
	) {
		var response = documentService.saveSharedWithMeDocumentToMyFiles(documentId, folderId);
		return ApiResponse.success("Save shared document to My Files successfully", response);
	}

	@PatchMapping("/{documentId}/visibility")
	public ApiResponse<DocumentUploadResponse> updateVisibility(
			@PathVariable Long documentId,
			@RequestParam("isPublic") Boolean isPublic
	) {
		var response = documentService.updateVisibility(documentId, isPublic);
		return ApiResponse.success("Update document visibility successfully", response);
	}

	@GetMapping("/document-share-approvals")
	public ApiResponse<Page<AdminDocumentShareApprovalResponse>> getDocumentShareApprovals(
			@RequestParam(defaultValue = "PENDING_APPROVAL") ShareApprovalStatus status,
			@RequestParam(required = false) DocumentShareApprovalType shareType,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ApiResponse.success(
				"Get document share approvals successfully",
				documentService.getDocumentShareApprovals(status, shareType, pageable)
		);
	}

	@GetMapping("/my/document-share-approvals")
	public ApiResponse<Page<UserDocumentShareApprovalResponse>> getMyApprovals(
			@RequestParam(required = false) ShareApprovalStatus status,
			@RequestParam(required = false) DocumentShareApprovalType shareType,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ApiResponse.success(
				"Get my document share approvals successfully",
				documentService.getMyDocumentShareApprovals(status, shareType, pageable)
		);
	}

	@PatchMapping("/{documentId}/share-approval")
	public ApiResponse<DocumentUploadResponse> reviewShareApproval(
			@PathVariable Long documentId,
			@RequestParam("status") ShareApprovalStatus status
	) {
		var response = documentService.reviewShareApproval(documentId, status);
		return ApiResponse.success("Review document share approval successfully", response);
	}

	@PatchMapping("/{documentId}/star")
	public ApiResponse<DocumentUploadResponse> updateStarred(
			@PathVariable Long documentId,
			@RequestParam("isStarred") Boolean isStarred
	) {
		var response = documentService.updateStarred(documentId, isStarred);
		return ApiResponse.success("Update document starred successfully", response);
	}

	@DeleteMapping("/{documentId}")
	public ApiResponse<DocumentUploadResponse> moveToTrash(@PathVariable Long documentId) {
		var response = documentService.moveToTrash(documentId);
		return ApiResponse.success("Move document to trash successfully", response);
	}

	@GetMapping("/trash")
	public ApiResponse<List<DocumentUploadResponse>> getTrash() {
		var response = documentService.getTrash();
		return ApiResponse.success("Get trash documents successfully", response);
	}

	@PostMapping("/{documentId}/restore")
	public ApiResponse<DocumentUploadResponse> restoreFromTrash(@PathVariable Long documentId) {
		var response = documentService.restoreFromTrash(documentId);
		return ApiResponse.success("Restore document successfully", response);
	}

	@DeleteMapping("/{documentId}/permanent")
	public ApiResponse<Void> deletePermanently(@PathVariable Long documentId) {
		try {
			documentService.deletePermanently(documentId);
			return ApiResponse.success("Delete document permanently successfully", null);
		// [SUA NGAY 2026-08-11 - co ho tro cua AI] Doi S3Exception -> FileStorageException,
		// cung ly do nhu cho upload o tren.
		} catch (FileStorageException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "File storage delete failed", ex);
		}
	}
}
