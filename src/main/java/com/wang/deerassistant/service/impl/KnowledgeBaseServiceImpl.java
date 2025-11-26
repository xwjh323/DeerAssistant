package com.wang.deerassistant.service.impl;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.entity.KnowledgeBase;
import com.wang.deerassistant.mapper.KnowledgeBaseMapper;
import com.wang.deerassistant.service.KnowledgeBaseService;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper kbMapper;
    private final PgVectorEmbeddingStore vectorStore;
    private final JdbcTemplate postgresJdbcTemplate;

    @Override
    public ApiResponse<?> listKb() {
        List<KnowledgeBase> list = kbMapper.selectList(null);
        return ResponseUtil.success(list);
    }

    @Override
    public ApiResponse<?> createKb(KnowledgeBase kb) {
        kbMapper.insert(kb);
        return ResponseUtil.success("知识库创建成功");
    }

    @Override
    public ApiResponse<?> updateKb(KnowledgeBase kb) {
        KnowledgeBase old = kbMapper.selectById(kb.getId());
        if (old == null) {
            return ResponseUtil.error("知识库不存在");
        }
        kbMapper.updateById(kb);
        return ResponseUtil.success("知识库更新成功");
    }

    @Override
    public ApiResponse<?> updateKb(Long id, String name, String description) {
        KnowledgeBase old = kbMapper.selectById(id);
        if (old != null) {
            return ResponseUtil.error("知识库不存在");
        }
        // 2. 构造要更新的对象（只更新传入的字段）
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setName(name);
        kb.setDescription(description);

        kbMapper.updateById(kb);
        return ResponseUtil.success("知识库更新成功");
    }

    @Override
    public ApiResponse<?> deleteKb(Long id) {
        int deleted = kbMapper.deleteById(id);

        if (deleted == 0) {
            return ResponseUtil.error("知识库不存在");
        }

        // MySQL 的外键已设置 ON DELETE CASCADE
        // 所以删除知识库会自动删除 knowledge_document 表
        // vectorStore 中的 docId/kbId 清理由 DocumentService 完成

        // 删除向量库
        String sql = """
                    SELECT embedding_id 
                    FROM deer_knowledge 
                    WHERE metadata->>'kbId' = ?
                """;

        List<String> ids = postgresJdbcTemplate.queryForList(sql, String.class, String.valueOf(id));
        vectorStore.removeAll(ids);
        log.info("已删除知识库 {} 下的 {} 条向量", id, ids.size());

        return ResponseUtil.success("知识库已删除");
    }

    @Override
    public ApiResponse<?> getKb(Long id) {
        KnowledgeBase knowledgeBase = kbMapper.selectById(id);
        return knowledgeBase == null ? ResponseUtil.error("知识库不存在")
                : ResponseUtil.success(knowledgeBase);
    }
}
