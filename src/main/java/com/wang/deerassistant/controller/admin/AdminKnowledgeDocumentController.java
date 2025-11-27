package com.wang.deerassistant.controller.admin;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.dto.PreviewSplitRequest;
import com.wang.deerassistant.dto.UploadDocumentRequest;
import com.wang.deerassistant.service.KnowledgeDocumentService;
import com.wang.deerassistant.service.SplitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/kb/doc")
@RequiredArgsConstructor
public class AdminKnowledgeDocumentController {

    private final KnowledgeDocumentService docService;
    private final SplitService splitService;

    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam Long kbId) {
        return docService.listDocs(kbId);
    }

    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestBody UploadDocumentRequest req) {
        return ResponseUtil.success(docService.uploadDocument(
                req.getKbId(),
                req.getTitle(),
                req.getContent(),
                req.getSplitConfig()
        ));
    }



    @DeleteMapping("/{docId}")
    public ApiResponse<?> delete(@PathVariable Long docId) {
        return docService.deleteDocument(docId);
    }

    @PostMapping("/re-embed/{docId}")
    public ApiResponse<?> reEmbed(@PathVariable Long docId) {
        return docService.reEmbedDocument(docId);
    }

    @PostMapping("/preview-split")
    public ApiResponse<?> previewSplit(@RequestBody PreviewSplitRequest req) {
        return ResponseUtil.success(splitService.previewSplit(req.getContent(), req.getSplitConfig()));
    }


}
