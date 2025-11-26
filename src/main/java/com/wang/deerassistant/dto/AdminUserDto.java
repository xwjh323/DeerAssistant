package com.wang.deerassistant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserDto {

    private Long id;
    private String username;
    private String avatar;

    private Integer role;          // 0=管理员, 1=用户
    private Integer status;        // 1=启用, 0=禁用

    private LocalDateTime createTime;
    private LocalDateTime lastLoginAt;
}
