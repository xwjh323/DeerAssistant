package com.wang.deerassistant.controller.admin;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.entity.KnowledgeBase;
import com.wang.deerassistant.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/kb")
@RequiredArgsConstructor
public class AdminKnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return kbService.listKb();
    }

    @PostMapping("/create")
    public ApiResponse<?> create(@RequestBody KnowledgeBase kb) {
        return kbService.createKb(kb);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        return kbService.deleteKb(id);
    }
}
