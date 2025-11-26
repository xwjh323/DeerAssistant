package com.wang.deerassistant.service.admin.impl;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.service.KnowledgeDocumentService;
import com.wang.deerassistant.service.admin.AdminKbDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminKbDocServiceImpl implements AdminKbDocService {
    private final KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public ApiResponse<?> listDocs(Long kbId) {
        return knowledgeDocumentService.listDocs(kbId);
    }

    @Override
    public ApiResponse<?> upload(Long kbId, String title, String content) {
        return knowledgeDocumentService.uploadDocument(kbId, title, content);
    }

    @Override
    public ApiResponse<?> delete(Long docId) {
        return knowledgeDocumentService.deleteDocument(docId);
    }

    @Override
    public ApiResponse<?> rebuild(Long docId) {
        return knowledgeDocumentService.reEmbedDocument(docId);
    }
}
