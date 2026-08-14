package com.se1908.group01.dto;

import java.time.Instant;

public class FileAccessUrlResponse {

	private String url;
	private Instant expiresAt;
	private String fileName;
	private String contentType;

	public FileAccessUrlResponse() {
	}

	public FileAccessUrlResponse(String url, Instant expiresAt, String fileName, String contentType) {
		this.url = url;
		this.expiresAt = expiresAt;
		this.fileName = fileName;
		this.contentType = contentType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
