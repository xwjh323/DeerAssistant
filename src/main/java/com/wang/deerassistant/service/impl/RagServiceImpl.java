package com.wang.deerassistant.service.impl;

import com.wang.deerassistant.service.RagService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {
    private final EmbeddingModel embeddingModel;
    private final PgVectorEmbeddingStore pgVectorEmbeddingStore;

    @Override
    public void addText(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        TextSegment segment = TextSegment.from(text);
        pgVectorEmbeddingStore.add(embedding,segment);
    }

    @Override
    public void addText(String text, Long kbId) {
        Embedding embedding = embeddingModel.embed(text).content();

        TextSegment segment = TextSegment.from(text);
        segment.metadata().put("kbId", String.valueOf(kbId));

        pgVectorEmbeddingStore.add(embedding, segment);
    }

    @Override
    public void addText(String text, Long kbId, Long docId) {
        Embedding embedding = embeddingModel.embed(text).content();

        TextSegment segment = TextSegment.from(text);
        segment.metadata()
                .put("kbId", String.valueOf(kbId))
                .put("docId", String.valueOf(docId));

        pgVectorEmbeddingStore.add(embedding, segment);
    }


}
