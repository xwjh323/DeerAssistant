package com.wang.deerassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
