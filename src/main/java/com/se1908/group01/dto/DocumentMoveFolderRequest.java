package com.se1908.group01.dto;

/** Request body được dùng bởi endpoint đưa tài liệu vào folder sau upload. */
public class DocumentMoveFolderRequest {

	private Long folderId;

	public Long getFolderId() {
		return folderId;
	}

	public void setFolderId(Long folderId) {
		this.folderId = folderId;
	}
}
