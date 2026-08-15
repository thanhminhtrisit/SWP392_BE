package com.se1908.group01.dto;

import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import java.time.Instant;
import java.util.List;

public record ChatSessionResponse(
		Long sessionId,
		String title,
		ChatMode mode,
		Long folderId,
		KnowledgePolicy policy,
		String model,
		Double temperature,
		List<Long> selectedDocumentIds,
		Instant createdAt,
		Instant updatedAt
) {
}
