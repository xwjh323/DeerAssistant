package com.wang.deerassistant.service;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.entity.KnowledgeBase;

public interface KnowledgeBaseService {

    ApiResponse<?> listKb();

    ApiResponse<?> createKb(KnowledgeBase kb);

    ApiResponse<?> updateKb(KnowledgeBase kb);

    ApiResponse<?> updateKb(Long id, String name, String description);

    ApiResponse<?> deleteKb(Long id);

    ApiResponse<?> getKb(Long id);  // 可选，Admin 用
}

