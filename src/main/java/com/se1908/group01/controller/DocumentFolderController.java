package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.DocumentFolderRequest;
import com.se1908.group01.dto.DocumentFolderResponse;
import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.service.DocumentFolderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/document-folders")
public class DocumentFolderController {

	private final DocumentFolderService documentFolderService;

	public DocumentFolderController(DocumentFolderService documentFolderService) {
		this.documentFolderService = documentFolderService;
	}

	@PostMapping
	public ApiResponse<DocumentFolderResponse> createFolder(@Valid @RequestBody DocumentFolderRequest request) {
		var response = documentFolderService.createFolder(request);
		return ApiResponse.success("Create document folder successfully", response);
	}

	@GetMapping
	public ApiResponse<List<DocumentFolderResponse>> getMyFolders() {
		var response = documentFolderService.getMyFolders();
		return ApiResponse.success("Get my document folders successfully", response);
	}

	@GetMapping("/starred")
	public ApiResponse<List<DocumentFolderResponse>> getStarredFolders() {
		var response = documentFolderService.getStarredFolders();
		return ApiResponse.success("Get starred document folders successfully", response);
	}

	@GetMapping("/trash")
	public ApiResponse<List<DocumentFolderResponse>> getTrashFolders() {
		var response = documentFolderService.getTrashFolders();
		return ApiResponse.success("Get trash document folders successfully", response);
	}

	@PatchMapping("/{folderId}")
	public ApiResponse<DocumentFolderResponse> updateFolder(
			@PathVariable Long folderId,
			@Valid @RequestBody DocumentFolderRequest request
	) {
		var response = documentFolderService.updateFolder(folderId, request);
		return ApiResponse.success("Update document folder successfully", response);
	}

	@PatchMapping("/{folderId}/star")
	public ApiResponse<DocumentFolderResponse> updateStarred(
			@PathVariable Long folderId,
			@RequestParam("isStarred") Boolean isStarred
	) {
		var response = documentFolderService.updateStarred(folderId, isStarred);
		return ApiResponse.success("Update document folder starred successfully", response);
	}

	@DeleteMapping("/{folderId}")
	public ApiResponse<Void> deleteFolder(@PathVariable Long folderId) {
		documentFolderService.deleteFolder(folderId);
		return ApiResponse.success("Delete document folder successfully", null);
	}

	@PatchMapping("/{folderId}/restore")
	public ApiResponse<DocumentFolderResponse> restoreFolder(@PathVariable Long folderId) {
		var response = documentFolderService.restoreFolder(folderId);
		return ApiResponse.success("Restore document folder successfully", response);
	}

	@DeleteMapping("/{folderId}/permanent")
	public ApiResponse<Void> permanentlyDeleteFolder(@PathVariable Long folderId) {
		documentFolderService.permanentlyDeleteFolder(folderId);
		return ApiResponse.success("Permanently delete document folder successfully", null);
	}

	@GetMapping("/{folderId}/documents")
	public ApiResponse<List<DocumentUploadResponse>> getFolderDocuments(@PathVariable Long folderId) {
		var response = documentFolderService.getFolderDocuments(folderId);
		return ApiResponse.success("Get document folder documents successfully", response);
	}
}
