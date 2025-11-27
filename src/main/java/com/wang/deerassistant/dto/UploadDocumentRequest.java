package com.wang.deerassistant.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UploadDocumentRequest {
    private Long kbId;
    private String title;
    private String content;

    // 分段参数
    private Map<String, Object> splitConfig;
}

