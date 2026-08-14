package com.se1908.group01.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;

@Entity
@Table(name = "document_tag")
/**
 * Entity trung gian liên kết tài liệu với tag thuộc user sau upload.
 */
public class DocumentTag {

	@EmbeddedId
	private DocumentTagId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("documentId")
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("tagId")
	@JoinColumn(name = "tag_id", nullable = false)
	private Tag tag;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public DocumentTagId getId() {
		return id;
	}

	public void setId(DocumentTagId id) {
		this.id = id;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
