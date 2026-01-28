package com.wang.deerassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wang.deerassistant.dto.ChatSessionDto;
import com.wang.deerassistant.entity.ChatHistory;
import com.wang.deerassistant.entity.ChatSession;
import com.wang.deerassistant.mapper.ChatHistoryMapper;
import com.wang.deerassistant.mapper.ChatSessionMapper;
import com.wang.deerassistant.service.ChatHistoryService;
import com.wang.deerassistant.service.OssStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatHistoryMapper mapper;
    private final ChatSessionMapper chatSessionMapper;
    private final OssStorageService ossStorageService;

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
    public void saveUserMessage(Long userId, String sessionId, String message, String imageUrl) {
        ChatHistory c = new ChatHistory();
        c.setUserId(userId);
        c.setSessionId(sessionId);
        c.setRole("user");
        c.setMessage(message);
        c.setImageUrl(imageUrl);
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
        List<ChatHistory> list = mapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getSessionId, sessionId)
                        .ne(ChatHistory::getRole, "system")
                        .orderByAsc(ChatHistory::getCreatedAt)
        );
        for (ChatHistory item : list) {
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                String signedUrl = ossStorageService.generateSignedUrlFromStored(imageUrl);
                if (signedUrl != null && !signedUrl.isBlank()) {
                    item.setImageUrl(signedUrl);
                }
            }
        }
        return list;
    }

    @Override
    public List<ChatSessionDto> listUserSessions(Long userId) {

        // 查询用户所有消息
        List<ChatHistory> list = mapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .orderByDesc(ChatHistory::getCreatedAt)
        );

        // 用 LinkedHashMap 按 session 分组，并保留最新顺序
        Map<String, ChatSessionDto> map = new LinkedHashMap<>();

        for (ChatHistory h : list) {
            String sid = h.getSessionId();

            if (!map.containsKey(sid)) {
                ChatSessionDto dto = new ChatSessionDto();
                dto.setSessionId(sid);
                dto.setLastMessage(h.getMessage());
                dto.setUpdatedAt(h.getCreatedAt());

                map.put(sid, dto);
            }
        }

        if (!map.isEmpty()) {
            List<ChatSession> sessions = chatSessionMapper.selectList(
                    new LambdaQueryWrapper<ChatSession>()
                            .eq(ChatSession::getUserId, userId)
                            .in(ChatSession::getSessionId, map.keySet())
            );
            for (ChatSession session : sessions) {
                ChatSessionDto dto = map.get(session.getSessionId());
                if (dto != null) {
                    dto.setTitle(session.getTitle());
                }
            }
        }

        return new ArrayList<>(map.values());
    }

    @Override
    public String createNewSession(Long userId) {

        String sessionId = UUID.randomUUID().toString();

        // 插入一条 system 消息作为会话标题
        ChatHistory chat = new ChatHistory();
        chat.setUserId(userId);
        chat.setSessionId(sessionId);
        chat.setRole("system");
        chat.setMessage("新对话");
        mapper.insert(chat);

        return sessionId;
    }

    @Override
    public void deleteSession(Long userId, String sessionId) {

        mapper.delete(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getSessionId, sessionId)
        );
    }

    @Override
    public List<ChatHistory> getLastTwoMessages(Long userId, String sessionId) {
        return mapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getSessionId, sessionId)
                        .orderByDesc(ChatHistory::getCreatedAt)
                        .last("LIMIT 2")
        );
    }

    @Override
    public void saveSystemMessage(Long userId, String sessionId, String message) {
        ChatHistory h = new ChatHistory();
        h.setUserId(userId);
        h.setSessionId(sessionId);
        h.setRole("system");
        h.setMessage(message);
        mapper.insert(h);
    }


}
