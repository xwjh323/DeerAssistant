package com.wang.deerassistant.service;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.entity.ModelConfig;

public interface ModelConfigService {

    ApiResponse<?> listModels();

    ApiResponse<?> create(ModelConfig config);

    ApiResponse<?> update(ModelConfig config);

    ApiResponse<?> delete(Long id);

    ApiResponse<?> setDefaultChat(Long id);

    ApiResponse<?> setDefaultEmbed(Long id);

    ApiResponse<?> testChatModel(Long id, String prompt);

    ApiResponse<?> testEmbeddingModel(Long id, String text);
}
