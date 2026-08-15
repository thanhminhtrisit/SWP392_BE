package com.se1908.group01.dto;

import com.se1908.group01.entity.DocumentChunk;

public class RetrievedChunk {

	private final DocumentChunk chunk;
	private final double score;

	public RetrievedChunk(DocumentChunk chunk, double score) {
		this.chunk = chunk;
		this.score = score;
	}

	public DocumentChunk getChunk() {
		return chunk;
	}

	public double getScore() {
		return score;
	}
}
