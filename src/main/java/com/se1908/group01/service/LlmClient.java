package com.se1908.group01.service;

import com.se1908.group01.dto.AiGenerationOptions;

public interface LlmClient {

    String generateAnswer(String prompt, AiGenerationOptions options);
}
