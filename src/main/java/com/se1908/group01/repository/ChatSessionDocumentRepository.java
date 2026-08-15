package com.se1908.group01.repository;

import com.se1908.group01.entity.ChatSessionDocument;
import com.se1908.group01.entity.ChatSessionDocumentId;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

/** Đọc các document được gắn vào session và phục vụ mapping selectedDocumentIds cho FE. */
public interface ChatSessionDocumentRepository
		extends JpaRepository<ChatSessionDocument, ChatSessionDocumentId> {

	List<ChatSessionDocument> findByChatSessionSessionIdOrderByDocumentDocumentId(Long sessionId);

	@Query("SELECT csd.document.documentId FROM ChatSessionDocument csd WHERE csd.chatSession.sessionId = :sessionId ORDER BY csd.document.documentId")
	List<Long> findDocumentIdsBySessionId(@Param("sessionId") Long sessionId);

	void deleteByDocumentDocumentId(Long documentId);
}
