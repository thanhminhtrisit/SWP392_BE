package com.se1908.group01.service;

import com.se1908.group01.entity.ChatMessage;
import com.se1908.group01.enums.ChatMessageRole;
import com.se1908.group01.enums.ChatMessageStatus;
import com.se1908.group01.repository.ChatMessageRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
/**
 * Đọc một cửa sổ nhỏ lịch sử chat đã COMPLETED để hỗ trợ câu hỏi follow-up.
 * Memory chỉ bổ sung ngữ cảnh hội thoại; nguồn sự thật vẫn là document chunks được truy xuất.
 */
public class ChatConversationMemoryService {

	public static final int MAX_MEMORY_MESSAGES = 5;

	private final ChatMessageRepository chatMessageRepository;

	public ChatConversationMemoryService(ChatMessageRepository chatMessageRepository) {
		this.chatMessageRepository = chatMessageRepository;
	}

	public List<Message> getRecentMessages(Long sessionId) {
		// Lấy tối đa 5 message mới nhất rồi đảo lại thứ tự cũ -> mới trước khi đưa vào prompt.
		List<ChatMessage> recentMessages = new ArrayList<>(
				chatMessageRepository.findTop5ByChatSessionSessionIdAndStatusOrderByCreatedAtDescMessageIdDesc(
						sessionId,
						ChatMessageStatus.COMPLETED
				)
		);
		Collections.reverse(recentMessages);

		var memory = MessageWindowChatMemory.builder()
				.chatMemoryRepository(new InMemoryChatMemoryRepository())
				.maxMessages(MAX_MEMORY_MESSAGES)
				.build();
		for (var message : recentMessages) {
			memory.add(sessionId.toString(), toSpringMessage(message));
		}
		return memory.get(sessionId.toString());
	}

	private Message toSpringMessage(ChatMessage message) {
		if (message.getRole() == ChatMessageRole.USER) {
			return new UserMessage(message.getContent());
		}
		return new AssistantMessage(message.getContent());
	}
}
