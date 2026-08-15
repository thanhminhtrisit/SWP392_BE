package com.se1908.group01.service;

import com.se1908.group01.dto.ChatMessageListResponse;
import com.se1908.group01.dto.ChatMessageResponse;
import com.se1908.group01.dto.ChatSessionResponse;
import com.se1908.group01.dto.CreateChatSessionRequest;
import com.se1908.group01.dto.SendChatMessageRequest;
import com.se1908.group01.dto.UpdateChatSessionRequest;
import java.util.List;

public interface ChatSessionService {

	ChatSessionResponse createSession(CreateChatSessionRequest request);

	List<ChatSessionResponse> getMySessions();

	ChatSessionResponse updateSession(Long sessionId, UpdateChatSessionRequest request);

	void deleteSession(Long sessionId);

	ChatMessageListResponse getMessages(Long sessionId, int page, int size);

	ChatMessageResponse sendMessage(Long sessionId, SendChatMessageRequest request);
}
