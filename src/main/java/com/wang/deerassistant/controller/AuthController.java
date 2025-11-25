package com.wang.deerassistant.controller;

import com.wang.deerassistant.annotation.LoginUser;
import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.dto.UserLoginRequest;
import com.wang.deerassistant.dto.UserLoginResponse;
import com.wang.deerassistant.dto.UserRegisterRequest;
import com.wang.deerassistant.service.AdminAuthService;
import com.wang.deerassistant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AdminAuthService adminAuthService;


    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody UserRegisterRequest request) {
        return userService.register(request);

    }

    @PostMapping("/login")
    public ApiResponse<UserLoginResponse> login(@RequestBody UserLoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/profile")
    public ApiResponse<?> profile(@LoginUser Long userId) {
        return ResponseUtil.success("当前用户 ID：" + userId);
    }

    @PostMapping("/admin/auth/login")
    public ApiResponse<UserLoginResponse> adminLogin(@RequestBody UserLoginRequest request) {
        return adminAuthService.login(request);
    }

}
