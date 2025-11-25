package com.wang.deerassistant.service.impl;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.entity.KnowledgeBase;
import com.wang.deerassistant.mapper.KnowledgeBaseMapper;
import com.wang.deerassistant.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper kbMapper;

    @Override
    public ApiResponse<?> listKb() {
        List<KnowledgeBase> list = kbMapper.selectList(null);
        return ResponseUtil.success(list);
    }

    @Override
    public ApiResponse<?> createKb(KnowledgeBase kb) {

        if (kb.getName() == null || kb.getName().trim().isEmpty()) {
            return ResponseUtil.error("知识库名称不能为空");
        }

        kbMapper.insert(kb);
        return ResponseUtil.success("知识库已创建");
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

        return ResponseUtil.success("知识库已删除");
    }
}
