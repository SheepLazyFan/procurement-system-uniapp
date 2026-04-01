package com.procurement.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传服务接口
 * <p>
 * TODO: 后续部署需迁移至腾讯云 COS，目前开发阶段使用本地存储（data/image/）
 * </p>
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @param type 文件类型（image / excel / qrcode）
     * @return { url, fileName }
     */
    Map<String, String> upload(MultipartFile file, String type);

    /**
     * 删除文件
     *
     * @param url 文件访问 URL
     */
    void delete(String url);
}
