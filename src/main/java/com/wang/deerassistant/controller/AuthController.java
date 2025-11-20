package com.wang.deerassistant.controller;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.dto.UserLoginRequest;
import com.wang.deerassistant.dto.UserLoginResponse;
import com.wang.deerassistant.dto.UserRegisterRequest;
import com.wang.deerassistant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody UserRegisterRequest request) {
        return userService.register(request);

    }

    @PostMapping("/login")
    public ApiResponse<UserLoginResponse> login(@RequestBody UserLoginRequest request) {
        return userService.login(request);
    }
}
