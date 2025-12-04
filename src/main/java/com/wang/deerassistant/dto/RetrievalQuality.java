package com.wang.deerassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RetrievalQuality {
    private int hitCount;
    private double top1Score;   // 如果拿不到分数则用 -1
}
