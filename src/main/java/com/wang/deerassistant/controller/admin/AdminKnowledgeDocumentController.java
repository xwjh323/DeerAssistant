package com.wang.deerassistant.controller.admin;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/doc")
@RequiredArgsConstructor
public class AdminKnowledgeDocumentController {

    private final KnowledgeDocumentService docService;

    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam Long kbId) {
        return docService.listDocs(kbId);
    }

    @PostMapping("/upload")
    public ApiResponse<?> upload(
            @RequestParam Long kbId,
            @RequestParam String title,
            @RequestParam String content
    ) {
        return docService.uploadDocument(kbId, title, content);
    }

    @DeleteMapping("/{docId}")
    public ApiResponse<?> delete(@PathVariable Long docId) {
        return docService.deleteDocument(docId);
    }

    @PostMapping("/re-embed/{docId}")
    public ApiResponse<?> reEmbed(@PathVariable Long docId) {
        return docService.reEmbedDocument(docId);
    }
}
