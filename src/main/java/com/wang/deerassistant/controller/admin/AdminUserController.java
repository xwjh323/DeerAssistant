package com.wang.deerassistant.controller.admin;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;

import com.wang.deerassistant.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // 用户列表（分页参数后面再加）
    @GetMapping
    public ApiResponse<?> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return adminUserService.listUsers(page, pageSize);
    }

    // 启用 / 禁用用户
    @PutMapping("/{userId}/status")
    public ApiResponse<?> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Integer status
    ) {
        return adminUserService.updateStatus(userId, status);
    }

    // 查询用户活跃度
    @GetMapping("/{userId}/stats")
    public ApiResponse<?> userStats(@PathVariable Long userId) {

        return adminUserService.userStats(userId);
    }
}
