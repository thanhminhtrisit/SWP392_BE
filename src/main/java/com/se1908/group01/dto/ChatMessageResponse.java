package com.se1908.group01.dto;

import com.se1908.group01.enums.ChatMessageRole;
import com.se1908.group01.enums.ChatMessageStatus;
import java.time.Instant;
import java.util.List;

/** Response của session message, gồm nội dung assistant và source citation đã lưu. */
public record ChatMessageResponse(
		Long messageId,
		ChatMessageRole role,
		String content,
		ChatMessageStatus status,
		Instant createdAt,
		List<ChatMessageSourceResponse> sources
) {
}
