package com.wang.deerassistant.service.admin;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.entity.KnowledgeBase;

public interface AdminKbBaseService {
    ApiResponse<?> listBases();
    ApiResponse<?> create(String name, String description);
    ApiResponse<?> update(Long id, String name, String description);
    ApiResponse<?> delete(Long id);
}

