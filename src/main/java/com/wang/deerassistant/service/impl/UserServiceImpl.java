package com.wang.deerassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.dto.UserLoginRequest;
import com.wang.deerassistant.dto.UserLoginResponse;
import com.wang.deerassistant.dto.UserRegisterRequest;
import com.wang.deerassistant.entity.User;
import com.wang.deerassistant.mapper.UserMapper;
import com.wang.deerassistant.service.UserService;
import com.wang.deerassistant.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public ApiResponse<Void> register(UserRegisterRequest request) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        Long count = userMapper.selectCount(wrapper);
        if (count != 0) {
            return ResponseUtil.error("用户名已存在");
        }

        // 2. 创建用户（密码加密）
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.insert(user);

        return ResponseUtil.success();
    }

    @Override
    public ApiResponse<UserLoginResponse> login(UserLoginRequest request) {
        // 1. 根据用户名查用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return ResponseUtil.error("用户名或密码错误");
        }

        // 2. 校验密码
        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!matches) {
            return ResponseUtil.error("用户名或密码错误");
        }

        // 3. 校验是否被禁用（防止旧 token 之外的登录）
        if (user.getStatus() != null && user.getStatus() == 0) {
            return ResponseUtil.error("账号已被禁用，请联系管理员");
        }

        // 4. 更新 last_login_at
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        // 5. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        UserLoginResponse resp = UserLoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(token)
                .role(user.getRole())
                .build();

        return ResponseUtil.success(resp);
    }

}
