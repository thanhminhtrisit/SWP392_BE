package com.se1908.group01.service.impl;

import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.service.AiChatClientService;
import com.se1908.group01.service.LlmClient;
import org.springframework.stereotype.Service;

@Service
/**
 * Adapter mức domain để ChatSessionService gọi LLM mà không phụ thuộc trực tiếp vào provider.
 */
public class LlmClientImpl implements LlmClient {

    private final AiChatClientService aiChatClientService;

    public LlmClientImpl(AiChatClientService aiChatClientService) {
        this.aiChatClientService = aiChatClientService;
    }

    @Override
    public String generateAnswer(String prompt, AiGenerationOptions options) {
        // Chuyển prompt đã build từ RAG sang adapter Spring AI và trả answer về service session.
        return aiChatClientService.ask(prompt, options);
    }
}
