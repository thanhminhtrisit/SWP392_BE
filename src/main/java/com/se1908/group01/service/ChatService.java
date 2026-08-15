package com.se1908.group01.service;

import com.se1908.group01.dto.ChatAskRequest;
import com.se1908.group01.dto.ChatAskResponse;

public interface ChatService {

	ChatAskResponse ask(ChatAskRequest request);
}
