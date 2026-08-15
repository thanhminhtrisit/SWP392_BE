package com.se1908.group01.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.se1908.group01.entity.ChatMessage;
import com.se1908.group01.enums.ChatMessageRole;
import com.se1908.group01.enums.ChatMessageStatus;
import com.se1908.group01.repository.ChatMessageRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatConversationMemoryServiceTest {

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Test
	void getRecentMessagesReturnsFiveMessagesInChronologicalOrder() {
		when(chatMessageRepository.findTop5ByChatSessionSessionIdAndStatusOrderByCreatedAtDescMessageIdDesc(
				1L,
				ChatMessageStatus.COMPLETED
		)).thenReturn(List.of(
				message(ChatMessageRole.USER, "message 5"),
				message(ChatMessageRole.ASSISTANT, "message 4"),
				message(ChatMessageRole.USER, "message 3"),
				message(ChatMessageRole.ASSISTANT, "message 2"),
				message(ChatMessageRole.USER, "message 1")
		));
		var service = new ChatConversationMemoryService(chatMessageRepository);

		var result = service.getRecentMessages(1L);

		assertEquals(5, result.size());
		assertEquals("message 1", result.get(0).getText());
		assertEquals("message 5", result.get(4).getText());
	}

	private ChatMessage message(ChatMessageRole role, String content) {
		var message = new ChatMessage();
		message.setRole(role);
		message.setContent(content);
		return message;
	}
}
