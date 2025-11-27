package com.wang.deerassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;
    private String title;
    private String content;
    private Integer status; // 1=启用 0=禁用

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String splitConfig;

}
