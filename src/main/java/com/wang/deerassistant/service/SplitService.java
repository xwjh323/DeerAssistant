package com.wang.deerassistant.service;

import java.util.List;
import java.util.Map;

public interface SplitService {
    List<?> previewSplit(String content, Map<String, Object> config);
}
