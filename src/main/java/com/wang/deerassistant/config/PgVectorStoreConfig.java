package com.wang.deerassistant.config;

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource.pgvector")
@Getter
@Setter
public class PgVectorStoreConfig {

    private String host;    // 对应 jdbc-url
    private Integer port;    // 对应 jdbc-url
    private String database;
    private String username;   // 对应 username
    private String password;   // 对应 password
    private String table;      // 对应 table
    private Integer dimension; // 对应 dimension

    @Bean
    public PgVectorEmbeddingStore pgVectorStore() {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)// ★ 使用 JDBC URL，而不是 host/port
                .user(username)
                .password(password)
                .table(table)
                .dimension(dimension)
                .build();
    }
}
