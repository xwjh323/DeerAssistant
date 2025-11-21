package com.wang.deerassistant.config;

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PgVectorStoreConfig {

    @Value("${spring.datasource.postgres.host}")
    private String host;

    @Value("${spring.datasource.postgres.port}")
    private Integer port;

    @Value("${spring.datasource.postgres.user}")
    private String user;

    @Value("${spring.datasource.postgres.password}")
    private String password;

    @Value("${spring.datasource.postgres.database}")
    private String database;

    @Value("${spring.datasource.postgres.table}")
    private String table;

    @Value("${spring.datasource.postgres.dimension}")
    private Integer dimension;

    @Bean
    public PgVectorEmbeddingStore pgVectorStore() {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .user(user)
                .password(password)
                .database(database)
                .table(table)
                .dimension(dimension)
                .build();
    }
}

