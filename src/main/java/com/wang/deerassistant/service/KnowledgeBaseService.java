package com.wang.deerassistant.service;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.entity.KnowledgeBase;

public interface KnowledgeBaseService {

    ApiResponse<?> listKb();
    ApiResponse<?> createKb(KnowledgeBase kb);
    ApiResponse<?> deleteKb(Long id);
}
