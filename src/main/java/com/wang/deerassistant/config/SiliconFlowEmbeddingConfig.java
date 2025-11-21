package com.wang.deerassistant.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiliconFlowEmbeddingConfig {

    @Value("${siliconflow.api-key}")
    private String apiKey;
    @Value("${siliconflow.embedding-model}")
    private String embeddingModel;
    @Value("${siliconflow.base-url}")
    private String baseUrl;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .baseUrl(baseUrl)
                .build();
    }
}
