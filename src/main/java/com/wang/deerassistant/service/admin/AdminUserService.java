package com.wang.deerassistant.service.admin;

import com.wang.deerassistant.common.ApiResponse;

public interface AdminUserService {

    ApiResponse<?> listUsers(int page, int pageSize);

    ApiResponse<?> updateStatus(Long userId, Integer status);

    ApiResponse<?> userStats(Long userId);
}
