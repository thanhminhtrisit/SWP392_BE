package com.se1908.group01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TagRequest {

	@NotBlank(message = "Tag name is required")
	@Size(max = 100, message = "Tag name must be at most 100 characters")
	private String name;

	@NotBlank(message = "Tag color is required")
	@Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Tag color must be a valid HEX color")
	private String color;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
}
