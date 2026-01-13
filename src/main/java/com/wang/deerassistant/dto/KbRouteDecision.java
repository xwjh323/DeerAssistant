package com.wang.deerassistant.dto;

import lombok.Data;

@Data
public class KbRouteDecision {
    private Long kbId;              // 选中的 kbId；可为 null 表示 NONE
    private double confidence;      // 0~1
    private String reason;          // 简短原因
    private boolean askUser;        // 这里你不对用户暴露，但用于内部策略
}
