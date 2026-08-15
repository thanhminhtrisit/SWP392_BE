package com.se1908.group01.service;

import com.se1908.group01.dto.AiGenerationOptions;

public interface AiChatClientService {

	String ask(String prompt, AiGenerationOptions options);
}
