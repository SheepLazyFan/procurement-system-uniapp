package com.procurement.service.impl;

import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.config.CosConfig;
import com.procurement.service.FileService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件上传服务实现
 * <p>
 * 开发阶段：COS 凭证为占位符时自动降级为本地存储（data/image/）<br>
 * TODO: 后续部署需迁移至腾讯云 COS，移除本地存储逻辑
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final COSClient cosClient;
    private final CosConfig cosConfig;

    /**
     * 本地文件存储根目录 — TODO: 部署后迁移至腾讯云 COS
     */
    @Value("${file.local-storage-dir:data/image}")
    private String localStorageDir;

    /**
     * 后端服务端口 + context-path，用于拼接本地文件访问 URL
     */
    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    /** 是否使用本地存储（COS 凭证为占位符时自动降级） */
    private boolean useLocalStorage = false;

    /** 允许上传的文件扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",  // 图片
            ".xlsx", ".xls", ".csv",                     // 表格
            ".pdf"                                       // 文档
    );

    /** 最大文件大小: 10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @PostConstruct
    public void init() {
        // 检测 COS 凭证是否为占位符 — 占位符则降级为本地存储
        String secretId = cosConfig.getSecretId();
        if (secretId == null || secretId.isBlank() || secretId.startsWith("your-")) {
            useLocalStorage = true;
            log.warn("⚠ COS 凭证未配置，降级为本地文件存储: {}", localStorageDir);
            // 确保本地存储目录存在
            try {
                Files.createDirectories(Paths.get(localStorageDir));
            } catch (Exception e) {
                log.error("创建本地存储目录失败: {}", localStorageDir, e);
            }
        } else {
            log.info("✅ 使用腾讯云 COS 文件存储");
        }
    }

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

        // 提取扩展名
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
        String relativePath = String.format("%s/%s/%s", type != null ? type : "file", dateDir, fileName);

        // TODO: 后续部署迁移 COS 后，移除 useLocalStorage 分支逻辑
        if (useLocalStorage) {
            return uploadToLocal(file, relativePath, originalFilename);
        } else {
            return uploadToCos(file, relativePath, originalFilename);
        }
    }

    /**
     * 上传到本地文件系统 — 开发阶段临时方案
     * TODO: 后续部署需迁移至腾讯云 COS
     */
    private Map<String, String> uploadToLocal(MultipartFile file, String relativePath, String originalFilename) {
        try {
            Path targetPath = Paths.get(localStorageDir, relativePath);
            Files.createDirectories(targetPath.getParent());
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 存储相对路径（不含域名），前端根据 BASE_URL 自行拼接
            String url = "/local-files/" + relativePath;

            Map<String, String> result = new LinkedHashMap<>();
            result.put("url", url);
            result.put("fileName", originalFilename);

            log.info("文件本地上传成功: {} → {}", originalFilename, targetPath);
            return result;

        } catch (Exception e) {
            log.error("本地文件上传失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传到腾讯云 COS
     */
    private Map<String, String> uploadToCos(MultipartFile file, String key, String originalFilename) {
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

            log.info("文件上传COS成功: {}", url);
            return result;

        } catch (Exception e) {
            log.error("COS文件上传失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件 URL 不能为空");
        }

        if (useLocalStorage) {
            deleteFromLocal(url);
        } else {
            deleteFromCos(url);
        }
    }

    private void deleteFromLocal(String url) {
        // URL 格式: http://localhost:8080/api/local-files/{relativePath}
        String marker = "/local-files/";
        int idx = url.indexOf(marker);
        if (idx < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "无效的本地文件 URL");
        }
        String relativePath = url.substring(idx + marker.length());

        // 路径遍历防护
        if (relativePath.contains("..")) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "非法文件路径");
        }

        try {
            Path filePath = Paths.get(localStorageDir, relativePath).normalize();
            // 确保文件在 localStorageDir 内
            if (!filePath.startsWith(Paths.get(localStorageDir).normalize())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "非法文件路径");
            }
            if (Files.deleteIfExists(filePath)) {
                log.info("本地文件已删除: {}", filePath);
            } else {
                log.warn("本地文件不存在: {}", filePath);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("本地文件删除失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件删除失败: " + e.getMessage());
        }
    }

    private void deleteFromCos(String url) {
        // URL 格式: https://{bucket}.cos.{region}.myqcloud.com/{key}
        String prefix = String.format("https://%s.cos.%s.myqcloud.com/",
                cosConfig.getBucket(), cosConfig.getRegion());
        if (!url.startsWith(prefix)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "无效的 COS 文件 URL");
        }
        String key = url.substring(prefix.length());

        if (key.contains("..")) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "非法文件路径");
        }

        try {
            cosClient.deleteObject(cosConfig.getBucket(), key);
            log.info("COS 文件已删除: {}", key);
        } catch (Exception e) {
            log.error("COS 文件删除失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件删除失败: " + e.getMessage());
        }
    }
}
