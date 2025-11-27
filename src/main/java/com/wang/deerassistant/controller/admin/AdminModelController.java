package com.wang.deerassistant.controller.admin;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.entity.ModelConfig;
import com.wang.deerassistant.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/model")
@RequiredArgsConstructor
public class AdminModelController {

    private final ModelConfigService service;

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return service.listModels();
    }

    @PostMapping("/create")
    public ApiResponse<?> create(@RequestBody ModelConfig config) {
        return service.create(config);
    }

    @PostMapping("/update")
    public ApiResponse<?> update(@RequestBody ModelConfig config) {
        return service.update(config);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        return service.delete(id);
    }

    @PostMapping("/default/chat/{id}")
    public ApiResponse<?> setDefaultChat(@PathVariable Long id) {
        return service.setDefaultChat(id);
    }

    @PostMapping("/default/embed/{id}")
    public ApiResponse<?> setDefaultEmbed(@PathVariable Long id) {
        return service.setDefaultEmbed(id);
    }

    @PostMapping("/test/chat/{id}")
    public ApiResponse<?> testChat(@PathVariable Long id,
                                   @RequestParam String prompt) {
        return service.testChatModel(id, prompt);
    }

    @PostMapping("/test/embed/{id}")
    public ApiResponse<?> testEmbed(@PathVariable Long id,
                                    @RequestParam String text) {
        return service.testEmbeddingModel(id, text);
    }
}
