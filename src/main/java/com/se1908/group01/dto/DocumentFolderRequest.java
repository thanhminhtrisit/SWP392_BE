package com.se1908.group01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DocumentFolderRequest {

	@NotBlank(message = "Folder name is required")
	@Size(max = 100, message = "Folder name must be at most 100 characters")
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
