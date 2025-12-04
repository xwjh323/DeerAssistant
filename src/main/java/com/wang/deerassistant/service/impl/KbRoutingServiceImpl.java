package com.wang.deerassistant.service.impl;

import com.wang.deerassistant.dto.KbRouteDecision;
import com.wang.deerassistant.entity.KnowledgeBase;
import com.wang.deerassistant.service.KbRoutingService;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KbRoutingServiceImpl implements KbRoutingService {

    private final ChatModel blockingChatLanguageModel;

    @Override
    public KbRouteDecision route(String question, List<KnowledgeBase> candidates) {

        // 候选为空 -> NONE
        if (candidates == null || candidates.isEmpty()) {
            KbRouteDecision d = new KbRouteDecision();
            d.setKbId(null);
            d.setConfidence(1.0);
            d.setReason("no_kb_candidates");
            d.setAskUser(false);
            return d;
        }

        // 组装候选（仅用 name/description/id）
        String kbList = candidates.stream()
                .map(kb -> String.format("- id=%d, name=%s, description=%s",
                        kb.getId(),
                        safe(kb.getName()),
                        safe(kb.getDescription())))
                .collect(Collectors.joining("\n"));

        // ✅ 强约束：只能输出 JSON，且 kbId 只能来自候选或 null
        String prompt = """
                你是一个“知识库路由器”，只负责在候选知识库中选择最适合回答用户问题的 kbId。
                你只能依据知识库的 name/description 来做选择。
                
                规则：
                1) 只能从候选 id 中选一个 kbId，或者选择 null（表示不使用知识库，NONE）。
                2) 如果不确定，confidence 要低，并将 askUser=true。
                3) 必须输出严格 JSON，不要输出任何额外文字。
                
                输出 JSON 格式：
                {"kbId": 1或null, "confidence": 0到1的小数, "reason":"简短原因", "askUser": true或false}
                
                用户问题：
                %s
                
                候选知识库：
                %s
                """.formatted(question, kbList);

        String raw = blockingChatLanguageModel.chat(prompt);

        // 简单 JSON 解析（不引入新依赖：用 very-light 解析）
        return parseDecision(raw);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").trim();
    }

    private KbRouteDecision parseDecision(String raw) {
        // 为了稳，做一个非常保守的解析：提取常见字段
        // 你也可以换成 Jackson ObjectMapper（项目里已经有）
        KbRouteDecision d = new KbRouteDecision();
        try {
            String json = raw.trim();
            // 粗暴兜底：确保是 {...}
            int l = json.indexOf('{');
            int r = json.lastIndexOf('}');
            if (l >= 0 && r > l) json = json.substring(l, r + 1);

            // 使用 Jackson（你项目已经有 com.fasterxml.jackson.databind.ObjectMapper 在 interceptor 用了）
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = om.readTree(json);

            if (node.has("kbId") && !node.get("kbId").isNull()) {
                d.setKbId(node.get("kbId").asLong());
            } else {
                d.setKbId(null);
            }
            d.setConfidence(node.has("confidence") ? node.get("confidence").asDouble() : 0.0);
            d.setReason(node.has("reason") ? node.get("reason").asText() : "");
            d.setAskUser(node.has("askUser") && node.get("askUser").asBoolean());
            return d;

        } catch (Exception e) {
            d.setKbId(null);
            d.setConfidence(0.0);
            d.setReason("route_parse_failed");
            d.setAskUser(true);
            return d;
        }
    }
}
