package com.wang.deerassistant.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiliconFlowStreamingConfig {

    @Value("${siliconflow.api-key}")
    private String apiKey;

    @Value("${siliconflow.model}")
    private String modelName;

    @Value("${siliconflow.base-url}")
    private String baseUrl;

    @Bean
    public StreamingChatModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)   // 指向 https://api.siliconflow.cn/v1
                .build();
    }
}
