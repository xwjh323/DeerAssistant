package com.wang.deerassistant.service;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.dto.UserLoginRequest;
import com.wang.deerassistant.dto.UserLoginResponse;

public interface AdminAuthService {

    ApiResponse<UserLoginResponse> login(UserLoginRequest request);
}
