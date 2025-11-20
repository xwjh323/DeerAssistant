package com.wang.deerassistant.service;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.dto.UserLoginRequest;
import com.wang.deerassistant.dto.UserLoginResponse;
import com.wang.deerassistant.dto.UserRegisterRequest;

public interface UserService {

    ApiResponse<Void> register(UserRegisterRequest request);

    ApiResponse<UserLoginResponse> login(UserLoginRequest request);
}
