package com.se1908.group01.service;

import com.se1908.group01.dto.DocumentFolderRequest;
import com.se1908.group01.dto.DocumentFolderResponse;
import com.se1908.group01.dto.DocumentUploadResponse;
import java.util.List;

public interface DocumentFolderService {

	DocumentFolderResponse createFolder(DocumentFolderRequest request);

	List<DocumentFolderResponse> getMyFolders();

	List<DocumentFolderResponse> getStarredFolders();

	List<DocumentFolderResponse> getTrashFolders();

	DocumentFolderResponse updateFolder(Long folderId, DocumentFolderRequest request);

	DocumentFolderResponse updateStarred(Long folderId, Boolean isStarred);

	void deleteFolder(Long folderId);

	DocumentFolderResponse restoreFolder(Long folderId);

	void permanentlyDeleteFolder(Long folderId);

	List<DocumentUploadResponse> getFolderDocuments(Long folderId);
}
