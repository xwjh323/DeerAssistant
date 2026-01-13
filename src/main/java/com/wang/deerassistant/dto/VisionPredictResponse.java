package com.wang.deerassistant.dto;

import lombok.Data;

import java.util.Map;

@Data
public class VisionPredictResponse {
    private String predict_class;
    private Double confidence;
    private Map<String,Double> scores;
}
