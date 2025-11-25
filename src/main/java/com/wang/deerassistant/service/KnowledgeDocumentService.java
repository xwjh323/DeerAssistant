package com.wang.deerassistant.service;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.entity.KnowledgeDocument;

public interface KnowledgeDocumentService {

    ApiResponse<?> listDocs(Long kbId);

    ApiResponse<?> uploadDocument(Long kbId, String title, String content);

    ApiResponse<?> deleteDocument(Long docId);

    ApiResponse<?> reEmbedDocument(Long docId);
}
