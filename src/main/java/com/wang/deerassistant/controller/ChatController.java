package com.wang.deerassistant.controller;

import com.wang.deerassistant.annotation.LoginUser;
import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.entity.ChatHistory;
import com.wang.deerassistant.service.ChatHistoryService;
import com.wang.deerassistant.service.ChatSessionService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final StreamingChatModel chatModel;
    private final ChatHistoryService chatHistoryService;
    private final ChatSessionService chatSessionService;
    private final EmbeddingModel embeddingModel;
    private final PgVectorEmbeddingStore pgVectorEmbeddingStore;

    /**
     * 流式聊天接口，带 sessionId
     */
    @GetMapping("/stream")
    public SseEmitter chatStream(
            @LoginUser Long userId,
            @RequestParam String question,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false, defaultValue = "0") Long kbId
    ) {

        // 如果 sessionId 为空，为用户自动创建一个
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        String finalSessionId = sessionId;

        // 1. 保存用户发出的问题
        chatHistoryService.saveUserMessage(userId, sessionId, question);

        // 2. 读取历史对话（真正开启上下文）
        List<ChatHistory> historyList = chatHistoryService.listBySessionId(userId, finalSessionId);

        List<ChatMessage> messages = new ArrayList<>();

        // 4. 将历史消息添加到上下文,最长6次对话
        int MAX_HISTORY = 6;

        int start = Math.max(0, historyList.size() - MAX_HISTORY);

        for (int i = start; i < historyList.size(); i++) {
            ChatHistory h = historyList.get(i);

            if ("user".equals(h.getRole())) {
                messages.add(UserMessage.from(h.getMessage()));
            } else {
                messages.add(AiMessage.from(h.getMessage()));
            }
        }


        // 5. 将用户最新提问加入上下文
        messages.add(UserMessage.from(question));

        // 6. 创建 SSE 流对象
        SseEmitter emitter = new SseEmitter(0L);

        StringBuilder aiAnswerBuilder = new StringBuilder();

        // ★ 先把 sessionId 主动返回给前端（关键）
        try {
            emitter.send(SseEmitter.event()
                    .name("session")
                    .data(finalSessionId));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        log.info("开始 RAG 检索：{}, kbId={}", question, kbId);

        // 1) 按 kbId 过滤
        Filter kbFilter = MetadataFilterBuilder
                .metadataKey("kbId")
                .isEqualTo(String.valueOf(kbId));

        // 2) 构造 retriever
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(pgVectorEmbeddingStore)
                .embeddingModel(embeddingModel)
                .filter(kbFilter)
                .maxResults(5)
                .build();

        // 3) 执行检索
        Query ragQuery = Query.from(question);
        List<Content> retrieved = retriever.retrieve(ragQuery);

        // 4) 安全获取 chunkIndex
        Comparator<Content> byChunkIndex = Comparator.comparing(c -> {
            String idx = c.metadata() != null ? (String) c.metadata().get("chunkIndex") : null;
            if (idx == null || idx.isEmpty()) return Integer.MAX_VALUE;
            try {
                return Integer.parseInt(idx);
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        });

        // 5) 安全获取 titlePath
        String ragContext = retrieved.stream()
                .sorted(byChunkIndex)
                .map(c -> {
                    String titlePath = "[]";
                    if (c.metadata() != null) {
                        String v = (String) c.metadata().get("titlePath");
                        if (v != null && !v.isEmpty()) titlePath = v;
                    }
                    return "[章节: " + titlePath + "]\n" + c.textSegment();
                })
                .collect(Collectors.joining("\n\n"));

        log.info("RAG 检索到 {} 条文档", retrieved.size());
        log.info("RAG 上下文：\n{}", ragContext);

        // ★ 6) 注入系统提示消息（RAG Context）
        if (!ragContext.isEmpty()) {
            messages.add(SystemMessage.from(
                    "你是一名鹿科动物识别专家，请根据以下知识库内容回答用户问题：\n\n"
                            + ragContext
            ));
        }

        // 7. 调用 AI 流式输出
        chatModel.chat(messages, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String token) {
                try {
                    aiAnswerBuilder.append(token);
                    emitter.send(SseEmitter.event().data(token));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {

                String finalAnswer = aiAnswerBuilder.toString();

                // 保存 AI 回复
                chatHistoryService.saveAiMessage(userId, finalSessionId, finalAnswer);

                // 自动生成标题
                String newTitle = chatSessionService.generateTitleIfNeeded(userId, finalSessionId);

                try {
                    emitter.send(SseEmitter.event().name("title").data(newTitle));
                } catch (Exception ignored) {
                }

                try {
                    emitter.send(SseEmitter.event().name("end").data("[DONE]"));
                } catch (Exception ignored) {
                }

                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(error);
            }
        });


        return emitter;
    }

    @GetMapping("/sessions")
    public ApiResponse<?> listSessions(@LoginUser Long userId) {
        return ResponseUtil.success(chatHistoryService.listUserSessions(userId));
    }

    // 返回某个会话的全部历史记录
    @GetMapping("/history")
    public ApiResponse<?> history(
            @LoginUser Long userId,
            @RequestParam String sessionId
    ) {
        List<ChatHistory> list = chatHistoryService.listBySessionId(userId, sessionId);
        return ResponseUtil.success(list);
    }

    @PostMapping("/session/new")
    public ApiResponse<?> newSession(@LoginUser Long userId) {

        String sessionId = chatHistoryService.createNewSession(userId);
        chatSessionService.createSession(userId, sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("title", "新对话");

        return ResponseUtil.success(result);
    }

    @DeleteMapping("/session")
    public ApiResponse<?> deleteSession(
            @LoginUser Long userId,
            @RequestParam String sessionId
    ) {
        chatHistoryService.deleteSession(userId, sessionId);
        chatSessionService.deleteSession(userId, sessionId);
        return ResponseUtil.success("会话已删除");
    }
}
