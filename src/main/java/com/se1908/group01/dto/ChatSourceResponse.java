package com.se1908.group01.dto;

public class ChatSourceResponse {

	private Long chunkId;
	private Integer chunkIndex;
	private Integer pageNumber;
	private Double score;

	public ChatSourceResponse() {
	}

	public ChatSourceResponse(Long chunkId, Integer chunkIndex, Integer pageNumber, Double score) {
		this.chunkId = chunkId;
		this.chunkIndex = chunkIndex;
		this.pageNumber = pageNumber;
		this.score = score;
	}

	public Long getChunkId() {
		return chunkId;
	}

	public void setChunkId(Long chunkId) {
		this.chunkId = chunkId;
	}

	public Integer getChunkIndex() {
		return chunkIndex;
	}

	public void setChunkIndex(Integer chunkIndex) {
		this.chunkIndex = chunkIndex;
	}

	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}
}
