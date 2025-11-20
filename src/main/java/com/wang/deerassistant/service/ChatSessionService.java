package com.wang.deerassistant.service;

import com.wang.deerassistant.entity.ChatSession;

public interface ChatSessionService {
    void createSession(Long userId, String sessionId);

    void updateTitle(Long userId, String sessionId, String title);

    ChatSession getSession(Long userId, String sessionId);

    String generateTitleIfNeeded(Long userId, String sessionId);

    String getTitle(Long userId, String sessionId);

    void deleteSession(Long userId, String sessionId);

}