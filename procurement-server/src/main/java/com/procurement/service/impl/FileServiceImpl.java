package com.procurement.service.impl;

import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.config.CosConfig;
import com.procurement.service.FileService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件上传服务实现 — 腾讯云 COS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final COSClient cosClient;
    private final CosConfig cosConfig;

    /** 允许上传的文件扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",  // 图片
            ".xlsx", ".xls", ".csv",                     // 表格
            ".pdf"                                       // 文档
    );

    /** 最大文件大小: 10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public Map<String, String> upload(MultipartFile file, String type) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上传文件不能为空");
        }

        // 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "文件大小不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // 生成文件路径: /{type}/{date}/{uuid}.{ext}
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // 文件类型白名单校验
        if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "不支持的文件类型，允许: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String key = String.format("%s/%s/%s", type != null ? type : "file", dateDir, fileName);

        try {
            InputStream inputStream = file.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putRequest = new PutObjectRequest(
                    cosConfig.getBucket(), key, inputStream, metadata);
            cosClient.putObject(putRequest);

            // 构建访问 URL
            String url = String.format("https://%s.cos.%s.myqcloud.com/%s",
                    cosConfig.getBucket(), cosConfig.getRegion(), key);

            Map<String, String> result = new LinkedHashMap<>();
            result.put("url", url);
            result.put("fileName", originalFilename);

            log.info("文件上传成功: {}", url);
            return result;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }
}
