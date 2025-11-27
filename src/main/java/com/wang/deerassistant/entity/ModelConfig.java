package com.wang.deerassistant.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String provider;
    private String apiKey;
    private String baseUrl;
    private String chatModel;
    private String embeddingModel;

    private Integer isDefaultChat;
    private Integer isDefaultEmbed;
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
