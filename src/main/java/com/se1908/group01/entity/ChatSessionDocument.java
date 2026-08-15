package com.se1908.group01.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_session_document")
/** Entity trung gian nối session với document; single-document tạo một dòng liên kết. */
public class ChatSessionDocument {

	@EmbeddedId
	private ChatSessionDocumentId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("sessionId")
	@JoinColumn(name = "session_id", nullable = false)
	private ChatSession chatSession;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("documentId")
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	public ChatSessionDocumentId getId() {
		return id;
	}

	public void setId(ChatSessionDocumentId id) {
		this.id = id;
	}

	public ChatSession getChatSession() {
		return chatSession;
	}

	public void setChatSession(ChatSession chatSession) {
		this.chatSession = chatSession;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}
}
