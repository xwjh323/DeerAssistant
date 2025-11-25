package com.wang.deerassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.entity.ModelConfig;
import com.wang.deerassistant.mapper.ModelConfigMapper;
import com.wang.deerassistant.service.ModelConfigService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper mapper;

    @Override
    public ApiResponse<?> listModels() {
        return ResponseUtil.success(mapper.selectList(null));
    }

    @Override
    public ApiResponse<?> create(ModelConfig config) {
        mapper.insert(config);
        return ResponseUtil.success("模型已添加");
    }

    @Override
    public ApiResponse<?> update(ModelConfig config) {
        mapper.updateById(config);
        return ResponseUtil.success("模型已更新");
    }

    @Override
    public ApiResponse<?> delete(Long id) {
        mapper.deleteById(id);
        return ResponseUtil.success("模型已删除");
    }

    @Override
    public ApiResponse<?> setDefaultChat(Long id) {

        // 清除原默认
        mapper.update(null,
                new UpdateWrapper<ModelConfig>().set("is_default_chat", 0));

        // 设置新默认
        ModelConfig config = new ModelConfig();
        config.setId(id);
        config.setIsDefaultChat(1);
        mapper.updateById(config);

        return ResponseUtil.success("默认聊天模型已更新");
    }

    @Override
    public ApiResponse<?> setDefaultEmbed(Long id) {

        mapper.update(null,
                new UpdateWrapper<ModelConfig>().set("is_default_embed", 0));

        ModelConfig config = new ModelConfig();
        config.setId(id);
        config.setIsDefaultEmbed(1);
        mapper.updateById(config);

        return ResponseUtil.success("默认 embedding 模型已更新");
    }


    /** 动态创建 ChatLanguageModel 进行测试 */
    @Override
    public ApiResponse<?> testChatModel(Long id, String prompt) {

        ModelConfig cfg = mapper.selectById(id);
        if (cfg == null) return ResponseUtil.error("模型不存在");

        try {
            ChatLanguageModel model = OpenAiChatModel.builder()
                    .apiKey(cfg.getApiKey())
                    .baseUrl(cfg.getBaseUrl())
                    .modelName(cfg.getChatModel())
                    .build();

            String result = model.generate(prompt);

            return ResponseUtil.success(result);

        } catch (Exception e) {
            return ResponseUtil.error("模型调用失败: " + e.getMessage());
        }
    }


    /** 动态 EmbeddingModel 测试 */
    @Override
    public ApiResponse<?> testEmbeddingModel(Long id, String text) {

        ModelConfig cfg = mapper.selectById(id);
        if (cfg == null) return ResponseUtil.error("模型不存在");

        try {
            EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(cfg.getApiKey())
                    .baseUrl(cfg.getBaseUrl())
                    .modelName(cfg.getEmbeddingModel())
                    .build();

            var response = embeddingModel.embed(text);
            var emb = response.content();

            Map<String, Object> result = new HashMap<>();
            result.put("vector", emb.vector());
            result.put("dimension", emb.dimension());

            return ResponseUtil.success(result);

        } catch (Exception e) {
            return ResponseUtil.error("Embedding 调用失败: " + e.getMessage());
        }
    }

}
