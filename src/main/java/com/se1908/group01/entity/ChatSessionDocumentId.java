package com.se1908.group01.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ChatSessionDocumentId implements Serializable {

	@Column(name = "session_id")
	private Long sessionId;

	@Column(name = "document_id")
	private Long documentId;

	public ChatSessionDocumentId() {
	}

	public ChatSessionDocumentId(Long sessionId, Long documentId) {
		this.sessionId = sessionId;
		this.documentId = documentId;
	}

	public Long getSessionId() {
		return sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}

	public Long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof ChatSessionDocumentId that)) {
			return false;
		}
		return Objects.equals(sessionId, that.sessionId)
				&& Objects.equals(documentId, that.documentId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sessionId, documentId);
	}
}
