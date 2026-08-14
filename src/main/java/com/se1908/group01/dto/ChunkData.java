package com.se1908.group01.dto;

public class ChunkData {

	private final int chunkIndex;
	private final Integer pageNumber;
	private final String content;

	public ChunkData(int chunkIndex, Integer pageNumber, String content) {
		this.chunkIndex = chunkIndex;
		this.pageNumber = pageNumber;
		this.content = content;
	}

	public int getChunkIndex() {
		return chunkIndex;
	}

	public Integer getPageNumber() {
		return pageNumber;
	}

	public String getContent() {
		return content;
	}
}

