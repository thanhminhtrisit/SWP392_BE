package com.se1908.group01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DocumentRenameRequest {

	@NotBlank(message = "Original file name is required")
	@Size(max = 512, message = "Original file name must be at most 512 characters")
	private String originalFileName;

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}
}
