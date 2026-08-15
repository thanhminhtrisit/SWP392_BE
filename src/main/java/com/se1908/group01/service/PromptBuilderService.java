package com.se1908.group01.service;

import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import java.util.List;
import org.springframework.ai.chat.messages.Message;

public interface PromptBuilderService {

    String buildDocumentQuestionPrompt(String question, List<RetrievedChunk> chunks);

    String buildMultiDocumentQuestionPrompt(ChatMode mode, String context, String question);

    String buildSessionQuestionPrompt(
            ChatMode mode,
            KnowledgePolicy knowledgePolicy,
            String context,
            List<Message> conversationMemory,
            String question
    );
}
