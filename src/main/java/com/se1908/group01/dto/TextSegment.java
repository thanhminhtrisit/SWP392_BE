package com.se1908.group01.dto;

public class TextSegment {

	private final String text;
	private final Integer pageNumber;

	public TextSegment(String text, Integer pageNumber) {
		this.text = text;
		this.pageNumber = pageNumber;
	}

	public String getText() {
		return text;
	}

	public Integer getPageNumber() {
		return pageNumber;
	}
}

