package com.wang.deerassistant.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RagRetrieverConfig {

    private final PgVectorEmbeddingStore pgVectorStore;
    private final EmbeddingModel embeddingModel;

    @Bean
    public ContentRetriever contentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(pgVectorStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.3)  // 可选：过滤不相关内容
                .build();
    }
}
