package com.wang.deerassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wang.deerassistant.entity.ChatHistory;
import com.wang.deerassistant.entity.ChatSession;
import com.wang.deerassistant.mapper.ChatSessionMapper;
import com.wang.deerassistant.service.ChatHistoryService;
import com.wang.deerassistant.service.ChatSessionService;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper mapper;
    private final ChatHistoryService chatHistoryService;
    private final ChatModel blockingChatLanguageModel;

    @Override
    public void createSession(Long userId, String sessionId) {
        ChatSession s = new ChatSession();
        s.setUserId(userId);
        s.setSessionId(sessionId);
        s.setTitle("新对话");
        s.setCurrentKbId(null);
        s.setSwitchCooldown(0);
        mapper.insert(s);
    }

    @Override
    public void updateTitle(Long userId, String sessionId, String title) {
        UpdateWrapper<ChatSession> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("session_id", sessionId)
                .set("title", title);

        mapper.update(null, wrapper);
    }

    @Override
    public ChatSession getSession(Long userId, String sessionId) {
        return mapper.selectOne(
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .eq("session_id", sessionId)
        );
    }

    @Override
    public String generateTitleIfNeeded(Long userId, String sessionId) {
        // ① 查询会话信息
        ChatSession session = getSession(userId, sessionId);
        if (session == null) {
            return "";
        }
        // 如果已有标题（不是 "新对话"），直接返回
        String oldTitle = session.getTitle();
        if (oldTitle != null && !oldTitle.trim().equals("新对话")) {
            return oldTitle;
        }
        // ② 获取最近两条对话，用于生成标题
        List<ChatHistory> last = chatHistoryService.getLastTwoMessages(userId, sessionId);
        if (last == null || last.isEmpty()) {
            return oldTitle; // 返回 "新对话"
        }
        String input = last.stream()
                .map(ChatHistory::getMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        if (input.trim().isEmpty()) {
            return oldTitle;
        }
        // ③ 构建标题 prompt
        String prompt =
                "请为以下对话生成一个简短清晰的中文标题，不超过12个字，不要标点符号。\n\n" +
                        input;
        String title = "";
        try {
            // ④ 调用模型
            title = blockingChatLanguageModel.chat(prompt);
        } catch (Exception e) {
            // 模型调用失败，不中断对话流程
            System.err.println("标题生成失败: " + e.getMessage());
        }
        // ⑤ 标题校验 + 兜底
        if (title == null || title.trim().isEmpty()) {
            // 模型不给标题 → 自动兜底标题
            title = "新的对话";
        }
        title = title.trim();
        // ⑥ 保存标题
        updateTitle(userId, sessionId, title);
        return title;
    }


    @Override
    public String getTitle(Long userId, String sessionId) {
        ChatSession session = getSession(userId, sessionId);
        return session != null ? session.getTitle() : "新对话";
    }

    @Override
    public void deleteSession(Long userId, String sessionId) {
        mapper.delete(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .eq(ChatSession::getSessionId, sessionId)
        );
    }

    @Override
    public void ensureSessionExists(Long userId, String sessionId) {
        ChatSession s = getSession(userId, sessionId);
        if (s == null) {
            createSession(userId, sessionId);
        }
    }

    @Override
    public void setCurrentKb(Long userId, String sessionId, Long kbId, boolean switched) {
        UpdateWrapper<ChatSession> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("session_id", sessionId)
                .set("current_kb_id", kbId);

        if (switched) {
            wrapper.set("last_switch_at", java.time.LocalDateTime.now())
                    .set("switch_cooldown", 8); // ✅ 默认冷却 8 轮
        }
        mapper.update(null, wrapper);
    }

    @Override
    public void tickCooldown(Long userId, String sessionId) {
        ChatSession s = getSession(userId, sessionId);
        if (s == null) return;
        Integer cd = s.getSwitchCooldown();
        if (cd == null) cd = 0;
        if (cd <= 0) return;

        UpdateWrapper<ChatSession> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("session_id", sessionId)
                .set("switch_cooldown", cd - 1);
        mapper.update(null, wrapper);
    }

}

