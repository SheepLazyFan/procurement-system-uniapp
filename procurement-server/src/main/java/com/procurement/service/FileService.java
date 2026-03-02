package com.procurement.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传服务接口 — 腾讯云 COS
 */
public interface FileService {

    /**
     * 上传文件到 COS
     *
     * @param file 文件
     * @param type 文件类型（image / excel）
     * @return { url, fileName }
     */
    Map<String, String> upload(MultipartFile file, String type);
}
