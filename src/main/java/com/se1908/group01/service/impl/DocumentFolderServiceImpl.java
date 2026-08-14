package com.se1908.group01.service.impl;

import com.se1908.group01.dto.DocumentFolderRequest;
import com.se1908.group01.dto.DocumentFolderResponse;
import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentFolder;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.DocumentFolderRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentFolderService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class DocumentFolderServiceImpl implements DocumentFolderService {

	private final CurrentUserService currentUserService;
	private final DocumentFolderRepository documentFolderRepository;
	private final DocumentRepository documentRepository;

	public DocumentFolderServiceImpl(
			CurrentUserService currentUserService,
			DocumentFolderRepository documentFolderRepository,
			DocumentRepository documentRepository
	) {
		this.currentUserService = currentUserService;
		this.documentFolderRepository = documentFolderRepository;
		this.documentRepository = documentRepository;
	}

	@Transactional
	@Override
	public DocumentFolderResponse createFolder(DocumentFolderRequest request) {
		var userId = currentUserService.getCurrentUserId();
		var name = normalizeName(request.getName());
		if (documentFolderRepository.existsByUserIdAndNameIgnoreCaseAndIsDeletedFalse(userId, name)) {
			throw new IllegalArgumentException("Folder name already exists");
		}

		var folder = new DocumentFolder();
		folder.setUserId(userId);
		folder.setName(name);
		return toResponse(documentFolderRepository.save(folder));
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentFolderResponse> getMyFolders() {
		var userId = currentUserService.getCurrentUserId();
		return documentFolderRepository.findByUserIdAndIsDeletedFalseOrderByNameAsc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentFolderResponse> getStarredFolders() {
		var userId = currentUserService.getCurrentUserId();
		return documentFolderRepository.findByUserIdAndIsStarredTrueAndIsDeletedFalseOrderByNameAsc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentFolderResponse> getTrashFolders() {
		var userId = currentUserService.getCurrentUserId();
		return documentFolderRepository.findByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	@Override
	public DocumentFolderResponse updateFolder(Long folderId, DocumentFolderRequest request) {
		var userId = currentUserService.getCurrentUserId();
		var folder = findOwnedActiveFolder(userId, folderId);
		var name = normalizeName(request.getName());
		if (documentFolderRepository.existsByUserIdAndNameIgnoreCaseAndFolderIdNotAndIsDeletedFalse(userId, name, folderId)) {
			throw new IllegalArgumentException("Folder name already exists");
		}

		folder.setName(name);
		return toResponse(documentFolderRepository.save(folder));
	}

	@Transactional
	@Override
	public DocumentFolderResponse updateStarred(Long folderId, Boolean isStarred) {
		if (isStarred == null) {
			throw new IllegalArgumentException("isStarred is required");
		}
		var userId = currentUserService.getCurrentUserId();
		var folder = findOwnedActiveFolder(userId, folderId);
		folder.setIsStarred(isStarred);
		return toResponse(documentFolderRepository.save(folder));
	}

	@Transactional
	@Override
	public void deleteFolder(Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var folder = findOwnedActiveFolder(userId, folderId);
		var now = Instant.now();
		folder.setIsDeleted(true);
		folder.setDeletedAt(now);
		documentFolderRepository.save(folder);
		documentRepository.softDeleteFolderDocuments(userId, folder.getFolderId(), now);
	}

	@Transactional
	@Override
	public DocumentFolderResponse restoreFolder(Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var folder = findOwnedFolder(userId, folderId);
		folder.setIsDeleted(false);
		folder.setDeletedAt(null);
		documentRepository.restoreFolderDocuments(userId, folder.getFolderId());
		return toResponse(documentFolderRepository.save(folder));
	}

	@Transactional
	@Override
	public void permanentlyDeleteFolder(Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var folder = findOwnedFolder(userId, folderId);
		var docs = documentRepository.findByUserIdAndFolderId(userId, folder.getFolderId());
		if (!docs.isEmpty()) {
			var folderDeletedAt = folder.getDeletedAt();
			var minDeletedAt = folderDeletedAt != null ? folderDeletedAt.minusSeconds(2) : Instant.EPOCH;
			var toDelete = new java.util.ArrayList<Document>();
			var toDetach = new java.util.ArrayList<Document>();
			for (var doc : docs) {
				if (folderDeletedAt != null && doc.getDeletedAt() != null && doc.getDeletedAt().isBefore(minDeletedAt)) {
					doc.setFolderId(null);
					toDetach.add(doc);
				} else {
					toDelete.add(doc);
				}
			}
			if (!toDetach.isEmpty()) {
				documentRepository.saveAll(toDetach);
			}
			if (!toDelete.isEmpty()) {
				documentRepository.deleteAll(toDelete);
			}
		}
		documentFolderRepository.delete(folder);
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getFolderDocuments(Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var folder = findOwnedFolder(userId, folderId);
		List<Document> docs;
		if (Boolean.TRUE.equals(folder.getIsDeleted())) {
			var minDeletedAt = folder.getDeletedAt() != null ? folder.getDeletedAt().minusSeconds(2) : Instant.EPOCH;
			docs = documentRepository.findByUserIdAndFolderIdAndIsDeletedTrueAndDeletedAtGreaterThanEqualOrderByUploadedAtDesc(
					userId,
					folder.getFolderId(),
					minDeletedAt
			);
		} else {
			docs = documentRepository.findByUserIdAndFolderIdAndIsDeletedFalseOrderByUploadedAtDesc(
					userId,
					folder.getFolderId()
			);
		}
		return docs.stream()
				.map(this::toDocumentResponse)
				.toList();
	}

	private DocumentFolder findOwnedActiveFolder(Long userId, Long folderId) {
		if (folderId == null) {
			throw new IllegalArgumentException("folderId is required");
		}
		return documentFolderRepository.findByFolderIdAndUserIdAndIsDeletedFalse(folderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
	}

	private DocumentFolder findOwnedFolder(Long userId, Long folderId) {
		if (folderId == null) {
			throw new IllegalArgumentException("folderId is required");
		}
		return documentFolderRepository.findByFolderIdAndUserId(folderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
	}

	private String normalizeName(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Folder name is required");
		}
		return name.trim();
	}

	private DocumentFolderResponse toResponse(DocumentFolder folder) {
		var response = new DocumentFolderResponse();
		response.setFolderId(folder.getFolderId());
		response.setUserId(folder.getUserId());
		response.setName(folder.getName());
		response.setIsStarred(folder.getIsStarred());
		response.setIsDeleted(folder.getIsDeleted());
		response.setDeletedAt(folder.getDeletedAt());
		response.setCreatedAt(folder.getCreatedAt());
		response.setUpdatedAt(folder.getUpdatedAt());
		return response;
	}

	private DocumentUploadResponse toDocumentResponse(Document doc) {
		var res = new DocumentUploadResponse();
		res.setDocumentId(doc.getDocumentId());
		res.setUserId(doc.getUserId());
		res.setFolderId(doc.getFolderId());
		res.setOriginalFileName(doc.getOriginalFileName());
		res.setS3Key(doc.getS3Key());
		res.setContentType(doc.getContentType());
		res.setFileSize(doc.getFileSize());
		res.setIsPublic(doc.getIsPublic());
		res.setIsDeleted(doc.getIsDeleted());
		res.setIsStarred(doc.getIsStarred());
		res.setStatus(doc.getStatus());
		res.setUploadedAt(doc.getUploadedAt());
		res.setDeletedAt(doc.getDeletedAt());
		return res;
	}
}
