package com.wang.deerassistant.controller;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final ChatLanguageModel siliconFlowModel;

    @GetMapping("/ask")
    public String ask(@RequestParam String q) {
        return siliconFlowModel.generate(q);
    }
}
