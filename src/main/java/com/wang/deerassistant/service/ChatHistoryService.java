package com.wang.deerassistant.service;

import com.wang.deerassistant.dto.ChatSessionDto;
import com.wang.deerassistant.entity.ChatHistory;

import java.util.List;

public interface ChatHistoryService {

    void saveUserMessage(Long userId, String sessionId, String message);

    void saveAiMessage(Long userId, String sessionId, String message);

    List<ChatHistory> listBySessionId(Long userId, String sessionId);

    List<ChatSessionDto> listUserSessions(Long userId);

    String createNewSession(Long userId);

    void deleteSession(Long userId, String sessionId);

    List<ChatHistory> getLastTwoMessages(Long userId, String sessionId);

    void saveSystemMessage(Long userId, String sessionId, String message);


}
