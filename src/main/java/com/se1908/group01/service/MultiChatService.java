package com.se1908.group01.service;

import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.MultiChatAskResponse;

public interface MultiChatService {

	MultiChatAskResponse askMulti(MultiChatAskRequest request);
}
