package com.wang.deerassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wang.deerassistant.entity.ChatHistory;
import com.wang.deerassistant.mapper.ChatHistoryMapper;
import com.wang.deerassistant.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatHistoryMapper mapper;

    @Override
    public void saveUserMessage(Long userId, String sessionId, String message) {
        ChatHistory c = new ChatHistory();
        c.setUserId(userId);
        c.setSessionId(sessionId);
        c.setRole("user");
        c.setMessage(message);
        mapper.insert(c);
    }

    @Override
    public void saveAiMessage(Long userId, String sessionId, String message) {
        ChatHistory c = new ChatHistory();
        c.setUserId(userId);
        c.setSessionId(sessionId);
        c.setRole("ai");
        c.setMessage(message);
        mapper.insert(c);
    }

    @Override
    public List<ChatHistory> listBySessionId(Long userId, String sessionId) {
        return mapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getSessionId, sessionId)
                        .orderByAsc(ChatHistory::getCreatedAt)
        );
    }

}
