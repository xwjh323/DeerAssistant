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

        // 3. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        UserLoginResponse resp = UserLoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
        // 4. 返回 token + 基本信息
        return ResponseUtil.success(resp);
    }
}
