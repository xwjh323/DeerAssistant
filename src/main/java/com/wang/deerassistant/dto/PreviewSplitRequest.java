package com.wang.deerassistant.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PreviewSplitRequest {

    private String content;

    // splitConfig: mode, maxLevel, chunkSize, includeTitlePath...
    private Map<String, Object> splitConfig;
}
