package com.se1908.group01.service.impl;

import com.se1908.group01.entity.Document;
import com.se1908.group01.enums.DocumentStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.DocumentFolderRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.DocumentAccessService;
import java.util.HashSet;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
/**
 * Tập trung kiểm tra document có được phép đưa vào AI chat hay không.
 * Đây là lớp bảo vệ backend độc lập với việc FE đã disable ô nhập khi document chưa READY.
 */
public class DocumentAccessServiceImpl implements DocumentAccessService {

	private final DocumentRepository documentRepository;
	private final DocumentFolderRepository documentFolderRepository;

	public DocumentAccessServiceImpl(
			DocumentRepository documentRepository,
			DocumentFolderRepository documentFolderRepository
	) {
		this.documentRepository = documentRepository;
		this.documentFolderRepository = documentFolderRepository;
	}

	@Override
	public Document getReadyDocumentForChat(Long userId, Long documentId) {
		// Single-document chat chỉ resolve document owner hoặc public; share riêng không được nhánh này mở quyền.
		if (documentId == null) {
			throw new IllegalArgumentException("Document ID is required");
		}

		var document = documentRepository.findByDocumentIdAndUserIdAndIsDeletedFalse(documentId, userId)
				.or(() -> documentRepository.findByDocumentIdAndIsPublicTrueAndIsDeletedFalse(documentId))
				.orElseThrow(() -> new ResourceNotFoundException("Document not found or not accessible"));

		if (document.getStatus() != DocumentStatus.READY) {
			// READY chứng minh ingestion đã tạo chunk/embedding để vector search có dữ liệu sử dụng.
			throw new IllegalArgumentException("Document is not ready for chat");
		}

		return document;
	}

	@Override
	public List<Document> getReadyDocumentsForChat(Long userId, List<Long> documentIds) {
		// Session SelectedDocuments phải resolve đủ toàn bộ danh sách client gửi, không được âm thầm bỏ document lỗi.
		if (documentIds == null || documentIds.isEmpty()) {
			throw new IllegalArgumentException("Document IDs are required for SelectedDocuments mode");
		}
		var distinctDocumentIds = documentIds.stream().distinct().toList();
		var requestedDocumentIds = new HashSet<>(distinctDocumentIds);
		var documents = documentRepository.findAccessibleDocumentsByIdsAndStatus(
				distinctDocumentIds,
				userId,
				DocumentStatus.READY
		);
		var resolvedDocumentIds = documents.stream()
				.map(Document::getDocumentId)
				.collect(java.util.stream.Collectors.toSet());
		if (!resolvedDocumentIds.equals(requestedDocumentIds)) {
			throw new IllegalArgumentException(
					"One or more selected documents are unavailable, not ready, or not accessible");
		}
		return documents;
	}

	/**
	 * Chọn query theo hai điều kiện độc lập: có giới hạn folder hay không và có lấy
	 * thêm public document hay không. Tham số includePublicDocuments chỉ được đọc,
	 * không bị gán lại trong method này.
	 */
	@Override
	public List<Document> getAllReadyDocumentsForUser(
			Long userId,
			@Nullable Long folderId,
			boolean includePublicDocuments
	) {
		if (folderId != null) {
			// Không cho phép dùng folder không tồn tại hoặc không thuộc user hiện tại.
			documentFolderRepository.findByFolderIdAndUserId(folderId, userId)
					.orElseThrow(() -> new ResourceNotFoundException("Document folder not found"));
			if (includePublicDocuments) {
				// Tài liệu của user trong folder đã chọn + mọi tài liệu public READY.
				return documentRepository.findOwnedFolderAndPublicDocumentsByStatus(
						userId,
						folderId,
						DocumentStatus.READY
				);
			}
			// Chỉ tài liệu READY thuộc user và nằm trong folder đã chọn.
			return documentRepository.findOwnedDocumentsByFolderAndStatus(
					userId,
					folderId,
					DocumentStatus.READY
			);
		}
		if (includePublicDocuments) {
			// Không giới hạn folder: lấy tài liệu của user + tài liệu public READY.
			return documentRepository.findAllAccessibleDocumentsByStatus(userId, DocumentStatus.READY);
		}
		// Không giới hạn folder và không lấy public: chỉ lấy toàn bộ tài liệu READY của user.
		return documentRepository.findOwnedDocumentsByStatus(userId, DocumentStatus.READY);
	}
}
