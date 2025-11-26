package com.wang.deerassistant.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.dto.AdminUserDto;
import com.wang.deerassistant.entity.User;
import com.wang.deerassistant.mapper.UserMapper;
import com.wang.deerassistant.service.admin.AdminUserService;
import com.wang.deerassistant.service.ChatHistoryService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final ChatHistoryService chatHistoryService;

    @Override
    public ApiResponse<?> listUsers(int page, int pageSize) {

        Page<User> p = new Page<>(page, pageSize);

        Page<User> result = userMapper.selectPage(
                p,
                new LambdaQueryWrapper<User>()
                        .orderByDesc(User::getCreateTime)
        );

        List<AdminUserDto> list = result.getRecords().stream().map(u -> {
            AdminUserDto dto = new AdminUserDto();
            BeanUtils.copyProperties(u, dto);
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", list);
        data.put("total", result.getTotal());

        return ResponseUtil.success(data);
    }

    @Override
    public ApiResponse<?> updateStatus(Long userId, Integer status) {

        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseUtil.error("用户不存在");
        }

        if (user.getRole() != null && user.getRole() == 0) {
            return ResponseUtil.error("不能禁用管理员账号");
        }

        user.setStatus(status);
        userMapper.updateById(user);

        return ResponseUtil.success("用户状态已更新");
    }

    @Override
    public ApiResponse<?> userStats(Long userId) {

        // 查询聊天条数
        List<?> history = chatHistoryService.listUserSessions(userId);
        // 注意：你需要后续增加统计接口，否则这里暂时显示“待开发”

        Map<String, Object> stats = new HashMap<>();
        stats.put("messageCount", history.size());
        stats.put("TODO", "后续补充对话次数、模型使用量、最近活跃时间等");

        return ResponseUtil.success(stats);
    }
}
