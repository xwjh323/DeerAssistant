package com.wang.deerassistant.service;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.entity.KnowledgeDocument;

import java.util.Map;

public interface KnowledgeDocumentService {

    ApiResponse<?> listDocs(Long kbId);

    ApiResponse<?> uploadDocument(Long kbId, String title, String content);

    ApiResponse<?> deleteDocument(Long docId);

    ApiResponse<?> reEmbedDocument(Long docId);

    Object uploadDocument(Long kbId, String title, String content, Map<String, Object> splitConfig);

}
