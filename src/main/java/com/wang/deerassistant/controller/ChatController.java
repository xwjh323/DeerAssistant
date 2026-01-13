package com.wang.deerassistant.controller;

import com.wang.deerassistant.annotation.LoginUser;
import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.dto.RetrievalQuality;
import com.wang.deerassistant.dto.VisionPredictResponse;
import com.wang.deerassistant.entity.ChatHistory;
import com.wang.deerassistant.entity.ChatSession;
import com.wang.deerassistant.entity.KnowledgeBase;
import com.wang.deerassistant.mapper.KnowledgeBaseMapper;
import com.wang.deerassistant.service.ChatHistoryService;
import com.wang.deerassistant.service.ChatSessionService;
import com.wang.deerassistant.service.KbRoutingService;
import com.wang.deerassistant.service.VisionRecognizeService;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KbRoutingService kbRoutingService;
    private final VisionRecognizeService visionRecognizeService;



    /**
     * 流式聊天接口，带 sessionId
     */
    @GetMapping("/stream")
    public SseEmitter chatStream(
            @LoginUser Long userId,
            @RequestParam String question,
            @RequestParam(required = false) String sessionId
    ) {

        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        String finalSessionId = sessionId;

        // ✅ 确保 chat_session 记录存在（否则 getSession 会 null）
        chatSessionService.ensureSessionExists(userId, finalSessionId);

        // ✅ 每轮递减冷却
        chatSessionService.tickCooldown(userId, finalSessionId);

        // 1. 保存用户消息
        chatHistoryService.saveUserMessage(userId, finalSessionId, question);

        return doStreamChat(userId,finalSessionId,question);
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

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamMultipart(
            @LoginUser Long userId,
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) MultipartFile file
    ) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        String finalSessionId = sessionId;

        // session 保障
        chatSessionService.ensureSessionExists(userId, finalSessionId);
        chatSessionService.tickCooldown(userId, finalSessionId);

        // 如果带图片：先识别，再构造增强 question
        VisionPredictResponse pred = null;
        if (file != null && !file.isEmpty()) {
            pred = visionRecognizeService.predict(file);

            String userText = (question == null || question.isBlank())
                    ? "请识别图中鹿科动物并科普。"
                    : question;

            // 只取 score 最高的类别
            String bestEn = pickBestLabel(pred);
            double bestScore = pickBestScore(pred, bestEn);
            String bestZh = mapToChinese(bestEn);

            // 用“中文+英文+分数”构造提示词（更利于中文知识库检索）
            question = buildQuestionWithPrediction(userText, bestZh, bestEn, bestScore);

            // 推荐：存一条 system 记录（中英都存，便于追溯）
            chatHistoryService.saveSystemMessage(userId, finalSessionId,
                    String.format("[VISION] predicted=%s(%s) score=%.4f",
                            bestZh, bestEn, bestScore));
        }

        // 保存用户消息（保存增强后的 question，保证可复现）
        chatHistoryService.saveUserMessage(userId, finalSessionId, question);

        // 复用你原来的 SSE/RAG 主流程
        return doStreamChat(userId, finalSessionId, question);
    }

    private SseEmitter doStreamChat(Long userId, String sessionId, String question) {
        // ✅ 这里直接把你原来 GET 方法中
        // 从 “2. 拉历史（最多6条）” 到 return emitter 的全部逻辑粘过来
        // 唯一注意：不要再生成 sessionId；直接用入参 sessionId
        // 2. 拉历史（最多6条）
        List<ChatHistory> historyList = chatHistoryService.listBySessionId(userId, sessionId);

        List<ChatMessage> messages = new ArrayList<>();
        int MAX_HISTORY = 6;
        int start = Math.max(0, historyList.size() - MAX_HISTORY);

        // ✅ 内部 KB 来源标注（对用户透明）
        // 我们用一个简单约定：若 role=ai，则在文本前加 [KB=<currentKbId|NONE>]
        // 由于历史表没存 sourceKbId，这里用“当前会话kb”做弱标注；
        // 若你想更准，需要把每条 AI 消息的 source_kb_id 单独存表（后续再升级）。
        ChatSession session = chatSessionService.getSession(userId, sessionId);
        Long sessionKb = session != null ? session.getCurrentKbId() : null;
        String kbTag = "[KB=" + (sessionKb == null ? "NONE" : sessionKb) + "] ";

        for (int i = start; i < historyList.size(); i++) {
            ChatHistory h = historyList.get(i);
            if ("user".equals(h.getRole())) {
                messages.add(UserMessage.from(h.getMessage()));
            } else if ("ai".equals(h.getRole())) {
                messages.add(AiMessage.from(kbTag + h.getMessage()));
            } else {
                // system 也塞进去（你之前有 system 记录）
                messages.add(SystemMessage.from(h.getMessage()));
            }
        }

        // 加入最新问题
        messages.add(UserMessage.from(question));

        SseEmitter emitter = new SseEmitter(0L);
        StringBuilder aiAnswerBuilder = new StringBuilder();

        // 先把 sessionId 回给前端（你原来就有）
        try {
            emitter.send(SseEmitter.event().name("session").data(sessionId));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // ✅ 选择 kbId（用户无感）
        Long chosenKbId = decideKbIdSilently(userId, sessionId, question);

        // ✅ 做检索（kbId=null => NONE 不检索）
        String ragContext = "";
        if (chosenKbId != null) {
            ragContext = retrieveContext(question, chosenKbId);
        }

        // ✅ 系统约束：跨 KB 历史事实不得直接当依据
        String routerGuard = """
            你是鹿科动物识别助手。
            注意：对话历史中可能包含形如 [KB=xxx] 的内部标记，这些标记仅表示该信息来源于某个知识库。
            规则：
            - 如果当前使用的知识库与历史标记不一致，则不得把历史中的具体事实当作确定依据，除非你能从本轮检索内容中再次确认。
            - 当本轮没有检索内容时（NONE），不要引用任何带 [KB=] 标记的事实作为确定结论，必要时用不确定表达并引导用户补充信息。
            """;

        messages.add(0, SystemMessage.from(routerGuard));

        if (ragContext != null && !ragContext.isBlank()) {
            messages.add(SystemMessage.from(
                    "请根据以下知识库检索内容回答用户问题：\n\n" + ragContext
            ));
        }

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

                // 保存 AI 回复（对用户透明，不保存 KB tag）
                chatHistoryService.saveAiMessage(userId, sessionId, finalAnswer);

                String newTitle = chatSessionService.generateTitleIfNeeded(userId, sessionId);
                try { emitter.send(SseEmitter.event().name("title").data(newTitle)); } catch (Exception ignored) {}
                try { emitter.send(SseEmitter.event().name("end").data("[DONE]")); } catch (Exception ignored) {}

                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                try { emitter.send(SseEmitter.event().name("error").data(error.getMessage())); } catch (Exception ignored) {}
                emitter.completeWithError(error);
            }
        });

        return emitter;
    }

    private String buildQuestionWithPrediction(String userText,
                                               String zhLabel,
                                               String enLabel,
                                               double score) {

        // 分数阈值：你可以自己调；低分就更谨慎
        if (score < 0.5) {
            return String.format("""
                用户补充：%s
                图像识别结果倾向于：%s（%s，置信度%.2f）。
                由于置信度较低，请你结合知识库：
                1) 给出该物种的关键形态特征与栖息环境；
                2) 提供与常见混淆鹿科的区分点；
                3) 提示用户补拍哪些特征可提高确定性；
                4) 结论用谨慎表述。
                """, userText, zhLabel, enLabel, score);
        }

        return String.format("""
            用户补充：%s
            图像识别结果：%s（%s，置信度%.2f）。
            请结合知识库输出：
            - 该物种典型形态特征（角型、体型、毛色、斑纹等）
            - 分布与栖息地
            - 易混淆物种区分要点
            - 面向公众的简短科普说明
            """, userText, zhLabel, enLabel, score);
    }

    private Long decideKbIdSilently(Long userId, String sessionId, String question) {

        ChatSession session = chatSessionService.getSession(userId, sessionId);
        Long currentKbId = session != null ? session.getCurrentKbId() : null;
        int cooldown = session != null && session.getSwitchCooldown() != null ? session.getSwitchCooldown() : 0;

        // 1) 若当前 kb 为空：直接路由一次（无感）
        if (currentKbId == null) {
            Long routed = routeAndMaybeSelect(question);
            // 绑定（不算切换）
            chatSessionService.setCurrentKb(userId, sessionId, routed, false);
            return routed;
        }

        // 2) 用当前 kb 做一次检索质量判断
        var q1 = measureRetrieval(question, currentKbId);

        // 质量好：不切
        if (isGood(q1)) {
            return currentKbId;
        }

        // 3) 质量差：若在冷却期，不切，直接 NONE 或继续用当前（这里选择 NONE 更稳）
        if (cooldown > 0) {
            return null; // NONE
        }

        // 4) 走路由（只看 description），得到候选 kb
        Long candidate = routeAndMaybeSelect(question);

        // 路由仍然是 NONE 或仍然是当前 kb：直接 NONE（避免胡答）
        if (candidate == null || candidate.equals(currentKbId)) {
            return null;
        }

        // 5) 验证候选 kb 的检索质量
        var q2 = measureRetrieval(question, candidate);

        // 候选显著更好：静默切换 + 进入冷却
        if (isSignificantlyBetter(q1, q2)) {
            chatSessionService.setCurrentKb(userId, sessionId, candidate, true);
            return candidate;
        }

        // 否则不切换：NONE
        return null;
    }

    private Long routeAndMaybeSelect(String question) {
        // 拉 kb 列表（你项目里已有 KnowledgeBaseService，但它返回 ApiResponse）
        // 这里用 mapper 直查更方便：你也可以封装个内部方法
        List<KnowledgeBase> bases = knowledgeBaseMapper.selectList(null);

        var decision = kbRoutingService.route(question, bases);

        // 置信度阈值
        if (decision.getConfidence() < 0.60) return null;

        // 防止模型返回不在列表中的 kbId
        Long kbId = decision.getKbId();
        if (kbId == null) return null;

        boolean exists = bases.stream().anyMatch(k -> k.getId().equals(kbId));
        return exists ? kbId : null;
    }

    private String retrieveContext(String question, Long kbId) {

        Filter kbFilter = MetadataFilterBuilder
                .metadataKey("kbId")
                .isEqualTo(String.valueOf(kbId));

        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(pgVectorEmbeddingStore)
                .embeddingModel(embeddingModel)
                .filter(kbFilter)
                .maxResults(5)
                .build();

        List<Content> retrieved = retriever.retrieve(Query.from(question));

        Comparator<Content> byChunkIndex = Comparator.comparing(c -> {
            String idx = c.metadata() != null ? (String) c.metadata().get("chunkIndex") : null;
            if (idx == null || idx.isEmpty()) return Integer.MAX_VALUE;
            try { return Integer.parseInt(idx); } catch (NumberFormatException e) { return Integer.MAX_VALUE; }
        });

        return retrieved.stream()
                .sorted(byChunkIndex)
                .map(c -> {
                    String titlePath = "[]";
                    if (c.metadata() != null) {
                        String v = (String) c.metadata().get("titlePath");
                        if (v != null && !v.isEmpty()) titlePath = v;
                    }
                    return "[章节: " + titlePath + "]\n" + c.textSegment().text();
                })
                .collect(Collectors.joining("\n\n"));
    }

    private RetrievalQuality measureRetrieval(String question, Long kbId) {
        try {
            Filter kbFilter = MetadataFilterBuilder
                    .metadataKey("kbId")
                    .isEqualTo(String.valueOf(kbId));

            // 使用 embeddingSearch 拿到分数（langchain4j store 支持）
            Embedding queryEmb = embeddingModel.embed(question).content();

            EmbeddingSearchRequest req = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmb)
                    .maxResults(3)
                    .filter(kbFilter)
                    .build();

            EmbeddingSearchResult<TextSegment> res = pgVectorEmbeddingStore.search(req);

            int hit = res.matches() == null ? 0 : res.matches().size();
            double top1 = -1;

            if (hit > 0 && res.matches().get(0) != null) {
                top1 = res.matches().get(0).score();
            }
            return new RetrievalQuality(hit, top1);

        } catch (Exception e) {
            return new RetrievalQuality(0, -1);
        }
    }

    private boolean isGood(RetrievalQuality q) {
        // ✅ 你可以调参：top1>=0.55 且命中>0 视为有效
        return q.getHitCount() > 0 && q.getTop1Score() >= 0.55;
    }

    private boolean isSignificantlyBetter(RetrievalQuality cur,
                                          RetrievalQuality cand) {
        if (cand.getHitCount() <= 0) return false;
        if (cand.getTop1Score() < 0) return false;

        // 当前完全没命中，候选命中且 top1>=0.55 => 更好
        if (cur.getHitCount() == 0) return cand.getTop1Score() >= 0.55;

        // 提升幅度阈值（防抖）
        return (cand.getTop1Score() - cur.getTop1Score()) >= 0.15;
    }

    private static final Map<String, String> EN2ZH = Map.ofEntries(
            Map.entry("David's Deer", "麋鹿"),
            Map.entry("Eld's Deer", "坡鹿"),
            Map.entry("Moose", "驼鹿"),
            Map.entry("Red Deer", "马鹿"),
            Map.entry("Reeves' Muntjac", "小麂"),      // 注意：你给的是 Reeve's（你的 class list 里可能是 Reeves'）
            Map.entry("Chinese Water Deer", "獐"),
            Map.entry("Roe Deer", "狍"),
            Map.entry("Sika Deer", "梅花鹿"),
            Map.entry("Tufted Deer", "毛冠鹿"),
            Map.entry("White-Lipped Deer", "白唇鹿"),
            Map.entry("Sambar Deer", "水鹿"),
            Map.entry("Red Muntjac", "赤麂")
    );
    private String pickBestLabel(VisionPredictResponse pred) {
        if (pred == null) return null;

        // 优先从 scores 选最大值（你要求的逻辑）
        if (pred.getScores() != null && !pred.getScores().isEmpty()) {
            return pred.getScores().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(pred.getPredict_class());
        }

        // fallback：没有 scores 就用 predicted_class
        return pred.getPredict_class();
    }

    private double pickBestScore(VisionPredictResponse pred, String bestLabel) {
        if (pred == null || bestLabel == null) return 0.0;
        if (pred.getScores() == null) return pred.getConfidence() == null ? 0.0 : pred.getConfidence();
        return pred.getScores().getOrDefault(bestLabel, pred.getConfidence() == null ? 0.0 : pred.getConfidence());
    }

    private String mapToChinese(String enLabel) {
        if (enLabel == null) return null;
        return EN2ZH.getOrDefault(enLabel, enLabel); // 找不到就原样返回
    }

}
