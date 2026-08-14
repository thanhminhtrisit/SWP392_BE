package com.se1908.group01.service.impl;

import com.se1908.group01.dto.TagRequest;
import com.se1908.group01.dto.TagResponse;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentTag;
import com.se1908.group01.entity.DocumentTagId;
import com.se1908.group01.entity.Tag;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.repository.DocumentTagRepository;
import com.se1908.group01.repository.TagRepository;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.TagService;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
/**
 * Quản lý tag thuộc user và các dòng liên kết document_tag được dùng sau upload.
 */
public class TagServiceImpl implements TagService {

	private final CurrentUserService currentUserService;
	private final TagRepository tagRepository;
	private final DocumentRepository documentRepository;
	private final DocumentTagRepository documentTagRepository;

	public TagServiceImpl(
			CurrentUserService currentUserService,
			TagRepository tagRepository,
			DocumentRepository documentRepository,
			DocumentTagRepository documentTagRepository
	) {
		this.currentUserService = currentUserService;
		this.tagRepository = tagRepository;
		this.documentRepository = documentRepository;
		this.documentTagRepository = documentTagRepository;
	}

	@Transactional
	@Override
	public TagResponse createTag(TagRequest request) {
		var userId = currentUserService.getCurrentUserId();
		var name = normalizeName(request.getName());
		var color = normalizeColor(request.getColor());
		if (tagRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
			throw new IllegalArgumentException("Tag name already exists");
		}

		var tag = new Tag();
		tag.setUserId(userId);
		tag.setName(name);
		tag.setColor(color);
		return toResponse(tagRepository.save(tag));
	}

	@Transactional(readOnly = true)
	@Override
	public List<TagResponse> getMyTags() {
		var userId = currentUserService.getCurrentUserId();
		return tagRepository.findByUserIdOrderByNameAsc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	@Override
	public TagResponse updateTag(Long tagId, TagRequest request) {
		var userId = currentUserService.getCurrentUserId();
		var tag = findOwnedTag(userId, tagId);
		var name = normalizeName(request.getName());
		var color = normalizeColor(request.getColor());
		if (tagRepository.existsByUserIdAndNameIgnoreCaseAndTagIdNot(userId, name, tagId)) {
			throw new IllegalArgumentException("Tag name already exists");
		}

		tag.setName(name);
		tag.setColor(color);
		return toResponse(tagRepository.save(tag));
	}

	@Transactional
	@Override
	public void deleteTag(Long tagId) {
		var userId = currentUserService.getCurrentUserId();
		var tag = findOwnedTag(userId, tagId);
		documentTagRepository.deleteByTagTagIdAndTagUserId(tag.getTagId(), userId);
		tagRepository.delete(tag);
	}

	@Transactional
	@Override
	/**
	 * Liên kết tag thuộc user với document active cũng thuộc user.
	 * Method được gọi sau upload vì document id do transaction upload sinh ra.
	 */
	public TagResponse addTagToDocument(Long documentId, Long tagId) {
		var userId = currentUserService.getCurrentUserId();
		var document = findOwnedActiveDocument(userId, documentId);
		var tag = findOwnedTag(userId, tagId);
		if (!documentTagRepository.existsByDocumentDocumentIdAndTagTagId(document.getDocumentId(), tag.getTagId())) {
			// Tránh tạo dòng trùng trong bảng liên kết nhiều-nhiều document_tag.
			var documentTag = new DocumentTag();
			documentTag.setId(new DocumentTagId(document.getDocumentId(), tag.getTagId()));
			documentTag.setDocument(document);
			documentTag.setTag(tag);
			documentTagRepository.save(documentTag);
		}
		return toResponse(tag);
	}

	@Transactional
	@Override
	public void removeTagFromDocument(Long documentId, Long tagId) {
		var userId = currentUserService.getCurrentUserId();
		var document = findOwnedActiveDocument(userId, documentId);
		var tag = findOwnedTag(userId, tagId);
		documentTagRepository.deleteByDocumentDocumentIdAndTagTagId(document.getDocumentId(), tag.getTagId());
	}

	@Transactional(readOnly = true)
	@Override
	public List<TagResponse> getDocumentTags(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		var document = findOwnedActiveDocument(userId, documentId);
		return getTagsByDocumentId(document.getDocumentId());
	}

	@Transactional(readOnly = true)
	@Override
	public List<TagResponse> getPublicDocumentTags(Long documentId) {
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		var document = documentRepository.findByDocumentIdAndIsPublicTrueAndIsDeletedFalse(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Public document not found"));
		return getTagsByDocumentId(document.getDocumentId());
	}

	private List<TagResponse> getTagsByDocumentId(Long documentId) {
		return documentTagRepository.findByDocumentDocumentIdOrderByTagNameAsc(documentId)
				.stream()
				.map(DocumentTag::getTag)
				.map(this::toResponse)
				.toList();
	}

	private Document findOwnedActiveDocument(Long userId, Long documentId) {
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentRepository.findByDocumentIdAndUserIdAndIsDeletedFalse(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found"));
	}

	private Tag findOwnedTag(Long userId, Long tagId) {
		if (tagId == null) {
			throw new IllegalArgumentException("tagId is required");
		}
		return tagRepository.findByTagIdAndUserId(tagId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
	}

	private String normalizeName(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Tag name is required");
		}
		return name.trim();
	}

	private String normalizeColor(String color) {
		if (!StringUtils.hasText(color)) {
			throw new IllegalArgumentException("Tag color is required");
		}
		return color.trim().toUpperCase(Locale.ROOT);
	}

	private TagResponse toResponse(Tag tag) {
		var response = new TagResponse();
		response.setTagId(tag.getTagId());
		response.setUserId(tag.getUserId());
		response.setName(tag.getName());
		response.setColor(tag.getColor());
		response.setCreatedAt(tag.getCreatedAt());
		return response;
	}
}
