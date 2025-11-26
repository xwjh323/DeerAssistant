package com.wang.deerassistant.dto;

import lombok.Data;

@Data
public class UploadDocumentRequest {
    private Long kbId;
    private String title;
    private String content;
}

