package com.wang.deerassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.dto.UserLoginRequest;
import com.wang.deerassistant.dto.UserLoginResponse;
import com.wang.deerassistant.entity.User;
import com.wang.deerassistant.mapper.UserMapper;
import com.wang.deerassistant.service.AdminAuthService;
import com.wang.deerassistant.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public ApiResponse<UserLoginResponse> login(UserLoginRequest request) {

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );

        if (user == null || user.getRole() != 0) {
            return ResponseUtil.error("无权限访问管理后台");
        }

        if (user.getStatus() != 1) {
            return ResponseUtil.error("管理员账号已被停用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseUtil.error("账号或密码错误");
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        UserLoginResponse resp = UserLoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();

        return ResponseUtil.success(resp);
    }
}
