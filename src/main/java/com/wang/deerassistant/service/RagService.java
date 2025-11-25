package com.wang.deerassistant.service;

public interface RagService {
    void addText(String text);

    void addText(String text, Long kbId);

    void addText(String text, Long kbId, Long docId);
}

