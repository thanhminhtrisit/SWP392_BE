package com.se1908.group01.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
/** Composite key cho một dòng liên kết trong document_tag. */
public class DocumentTagId implements Serializable {

	@Column(name = "document_id")
	private Long documentId;

	@Column(name = "tag_id")
	private Long tagId;

	public DocumentTagId() {
	}

	public DocumentTagId(Long documentId, Long tagId) {
		this.documentId = documentId;
		this.tagId = tagId;
	}

	public Long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}

	public Long getTagId() {
		return tagId;
	}

	public void setTagId(Long tagId) {
		this.tagId = tagId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DocumentTagId that)) {
			return false;
		}
		return Objects.equals(documentId, that.documentId) && Objects.equals(tagId, that.tagId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(documentId, tagId);
	}
}
