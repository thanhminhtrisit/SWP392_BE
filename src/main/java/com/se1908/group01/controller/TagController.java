package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.TagRequest;
import com.se1908.group01.dto.TagResponse;
import com.se1908.group01.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
/**
 * Cung cấp các API tag để UploadModal tải, tạo và gắn tag cho tài liệu mới.
 */
public class TagController {

	private final TagService tagService;

	public TagController(TagService tagService) {
		this.tagService = tagService;
	}

	@PostMapping("/tags")
	public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
		var response = tagService.createTag(request);
		return ApiResponse.success("Create tag successfully", response);
	}

	@GetMapping("/tags")
	public ApiResponse<List<TagResponse>> getMyTags() {
		var response = tagService.getMyTags();
		return ApiResponse.success("Get my tags successfully", response);
	}

	@PatchMapping("/tags/{tagId}")
	public ApiResponse<TagResponse> updateTag(@PathVariable Long tagId, @Valid @RequestBody TagRequest request) {
		var response = tagService.updateTag(tagId, request);
		return ApiResponse.success("Update tag successfully", response);
	}

	@DeleteMapping("/tags/{tagId}")
	public ApiResponse<Void> deleteTag(@PathVariable Long tagId) {
		tagService.deleteTag(tagId);
		return ApiResponse.success("Delete tag successfully", null);
	}

	@PostMapping("/documents/{documentId}/tags/{tagId}")
	/** Luồng upload gọi endpoint này sau khi metadata tài liệu đã được tạo. */
	public ApiResponse<TagResponse> addTagToDocument(@PathVariable Long documentId, @PathVariable Long tagId) {
		var response = tagService.addTagToDocument(documentId, tagId);
		return ApiResponse.success("Add tag to document successfully", response);
	}

	@DeleteMapping("/documents/{documentId}/tags/{tagId}")
	public ApiResponse<Void> removeTagFromDocument(@PathVariable Long documentId, @PathVariable Long tagId) {
		tagService.removeTagFromDocument(documentId, tagId);
		return ApiResponse.success("Remove tag from document successfully", null);
	}

	@GetMapping("/documents/{documentId}/tags")
	public ApiResponse<List<TagResponse>> getDocumentTags(@PathVariable Long documentId) {
		var response = tagService.getDocumentTags(documentId);
		return ApiResponse.success("Get document tags successfully", response);
	}

	@GetMapping("/documents/public/{documentId}/tags")
	public ApiResponse<List<TagResponse>> getPublicDocumentTags(@PathVariable Long documentId) {
		var response = tagService.getPublicDocumentTags(documentId);
		return ApiResponse.success("Get public document tags successfully", response);
	}
}
