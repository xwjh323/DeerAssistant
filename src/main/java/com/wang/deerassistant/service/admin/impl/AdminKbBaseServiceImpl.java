package com.wang.deerassistant.service.admin.impl;

import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.entity.KnowledgeBase;
import com.wang.deerassistant.service.KnowledgeBaseService;
import com.wang.deerassistant.service.admin.AdminKbBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminKbBaseServiceImpl implements AdminKbBaseService {

    private final KnowledgeBaseService kbService;

    @Override
    public ApiResponse<?> listBases() {
        return kbService.listKb();
    }

    @Override
    public ApiResponse<?> create(String name, String description) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        return kbService.createKb(kb);
    }

    @Override
    public ApiResponse<?> update(Long id, String name, String description) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(description);
        return kbService.updateKb(knowledgeBase);
    }

    @Override
    public ApiResponse<?> delete(Long id) {
        return kbService.deleteKb(id);
    }
}
