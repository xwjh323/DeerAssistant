package com.wang.deerassistant.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChunkPreview {

    private Integer index;
    private List<String> titlePath;
    private String content;
    private Integer estimatedTokens;
}
