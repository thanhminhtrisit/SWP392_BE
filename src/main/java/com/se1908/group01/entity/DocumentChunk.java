package com.se1908.group01.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "document_chunk")
/**
 * Lưu một text chunk có thể tìm kiếm và embedding vector đã serialize của tài liệu.
 */
public class DocumentChunk {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chunk_id")
	private Long chunkId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	@Column(name = "chunk_index", nullable = false)
	private Integer chunkIndex;

	@Lob
	@Column(name = "content", nullable = false)
	private String content;

	@Column(name = "page_number")
	private Integer pageNumber;

	@Lob
	@Column(name = "embedding_vector")
	private String embeddingVector;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getChunkId() {
		return chunkId;
	}

	public void setChunkId(Long chunkId) {
		this.chunkId = chunkId;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public Integer getChunkIndex() {
		return chunkIndex;
	}

	public void setChunkIndex(Integer chunkIndex) {
		this.chunkIndex = chunkIndex;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	public String getEmbeddingVector() {
		return embeddingVector;
	}

	public void setEmbeddingVector(String embeddingVector) {
		this.embeddingVector = embeddingVector;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
