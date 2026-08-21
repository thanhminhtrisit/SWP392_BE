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

	// [SUA NGAY 2026-08-20 - co ho tro cua AI] Bo @Lob, them columnDefinition NVARCHAR(MAX).
	//
	// LY DO: cot nay truoc la varchar(max) nen mat dau tieng Viet (do tren doc 30008: 38 dau
	// '?' tren 8/44 chunk). Van ban o day duoc dua THANG vao prompt cua LLM nen AI tra loi
	// sai ten nguoi trong tai lieu. Migration V3 doi cot sang NVARCHAR(MAX).
	//
	// VI SAO BO @Lob: lam theo dung mau cua ChatMessage.content - cot da la nvarchar(max) va
	// dang chay tot - dung columnDefinition, khong dung @Lob. @Lob map sang kieu CLOB, de
	// lech voi NVARCHAR(MAX) khi Hibernate chay ddl-auto: validate.
	//
	// Luu y: @Lob o truong embeddingVector ben duoi GIU NGUYEN. Cot do chua chuoi JSON toan
	// chu so (ASCII thuan), co y giu varchar(max) vi doi sang nvarchar se gap doi dung luong.
	@Column(name = "content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
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
