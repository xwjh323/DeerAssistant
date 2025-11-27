package com.wang.deerassistant.service.admin;

import com.wang.deerassistant.common.ApiResponse;

public interface AdminKbDocService {
    ApiResponse<?> listDocs(Long kbId);
    ApiResponse<?> upload(Long kbId, String title, String content);
    ApiResponse<?> delete(Long docId);
    ApiResponse<?> rebuild(Long docId);
}
