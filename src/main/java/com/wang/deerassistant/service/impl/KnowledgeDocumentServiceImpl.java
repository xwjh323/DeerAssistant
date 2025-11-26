package com.wang.deerassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.entity.KnowledgeDocument;
import com.wang.deerassistant.mapper.KnowledgeDocumentMapper;
import com.wang.deerassistant.service.KnowledgeDocumentService;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;

import dev.langchain4j.data.document.splitter.*;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper docMapper;
    private final EmbeddingModel embeddingModel;
    private final PgVectorEmbeddingStore vectorStore;
    private final JdbcTemplate postgresJdbcTemplate;

    @Override
    public ApiResponse<?> listDocs(Long kbId) {
        List<KnowledgeDocument> list = docMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getKbId, kbId)
                        .orderByDesc(KnowledgeDocument::getUpdatedAt)
        );
        return ResponseUtil.success(list);
    }

    @Override
    public ApiResponse<?> uploadDocument(Long kbId, String title, String content) {

        // 1. 插入 MySQL
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKbId(kbId);
        doc.setTitle(title);
        doc.setContent(content);
        docMapper.insert(doc);

        Long docId = doc.getId();

        // 2. 构造层级拆分器（段落 > 句子 > 单词）
        DocumentSplitter splitter = DocumentSplitters.recursive(300,50);

        // 3. 正确构造 Document
        Document baseDoc = Document.from(content);

        List<TextSegment> segments = splitter.split(baseDoc);

        log.info("文档({}) 拆分为 {} 个 chunks", docId, segments.size());

        // 4. 向量化并写入 pgvector
        for (TextSegment seg : segments) {

            // 写 metadata
            seg.metadata()
                    .put("kbId", String.valueOf(kbId))
                    .put("docId", String.valueOf(docId));

            // embedding
            Embedding emb = embeddingModel.embed(seg.text()).content();

            // 写入向量库
            vectorStore.add(emb, seg);
        }

        return ResponseUtil.success("文档上传并已向量化");
    }

    @Override
    public ApiResponse<?> deleteDocument(Long docId) {

        KnowledgeDocument doc = docMapper.selectById(docId);
        if (doc == null) return ResponseUtil.error("文档不存在");

        docMapper.deleteById(docId);

        // 1. 查询所有 embedding_id
        String sql = """
        SELECT embedding_id 
        FROM deer_knowledge 
        WHERE metadata->>'docId' = ?
        """;

        List<String> ids = postgresJdbcTemplate.queryForList(
                sql,
                String.class,
                String.valueOf(docId)
        );

// 2. 删除向量
        vectorStore.removeAll(ids);

        log.info("已从向量库删除 {} 条 chunk", ids.size());



        return ResponseUtil.success("文档与向量已删除");
    }

    @Override
    public ApiResponse<?> reEmbedDocument(Long docId) {

        KnowledgeDocument doc = docMapper.selectById(docId);
        if (doc == null) return ResponseUtil.error("文档不存在");

        Long kbId = doc.getKbId();
        String content = doc.getContent();

        // 删除旧向量
        String sql = """
        SELECT embedding_id 
        FROM deer_knowledge 
        WHERE metadata->>'docId' = ?
        """;

        List<String> ids = postgresJdbcTemplate.queryForList(
                sql,
                String.class,
                String.valueOf(docId)
        );
        vectorStore.removeAll(ids);

        // 构造 splitter
        DocumentSplitter splitter = DocumentSplitters.recursive(300,50);
        Document baseDoc = Document.from(content);

        List<TextSegment> segments = splitter.split(baseDoc);

        // 重向量化
        for (TextSegment seg : segments) {
            seg.metadata()
                    .put("kbId", String.valueOf(kbId))
                    .put("docId", String.valueOf(docId));

            Embedding emb = embeddingModel.embed(seg.text()).content();
            vectorStore.add(emb, seg);
        }

        return ResponseUtil.success("文档重新向量化完成");
    }
}
