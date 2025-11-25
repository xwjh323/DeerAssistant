package com.wang.deerassistant.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiliconFlowConfig {

    @Value("${siliconflow.api-key}")
    private String apiKey;

    @Value("${siliconflow.model}")
    private String modelName;

    @Value("${siliconflow.base-url}")
    private String baseUrl;

    @Bean
    public ChatModel siliconFlowModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)   // ✔ 关键：接入第三方 OpenAI 接口平台
                .build();
    }
}
