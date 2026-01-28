package com.wang.deerassistant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionDto {

    private String sessionId;
    private String lastMessage;
    private String title;
    private LocalDateTime updatedAt;
}
