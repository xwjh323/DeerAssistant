package com.wang.deerassistant.controller;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/add")
    public ApiResponse<?> add(@RequestBody String text) {
        ragService.addText(text);
        return ResponseUtil.success("知识已添加");
    }
}
