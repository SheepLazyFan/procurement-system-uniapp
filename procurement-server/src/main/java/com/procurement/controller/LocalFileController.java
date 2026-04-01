package com.procurement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 本地文件静态资源控制器 — 开发阶段使用
 * <p>
 * 对外提供 GET /local-files/** 来访问 data/image/ 目录下的文件<br>
 * TODO: 后续部署需迁移至腾讯云 COS，此控制器将废弃
 * </p>
 */
@Slf4j
@Tag(name = "本地文件访问（开发阶段）")
@RestController
@RequestMapping("/local-files")
public class LocalFileController {

    /**
     * 本地文件存储根目录 — TODO: 部署后迁移至腾讯云 COS
     */
    @Value("${file.local-storage-dir:data/image}")
    private String localStorageDir;

    /** 扩展名 → Content-Type 映射 */
    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            ".jpg", MediaType.IMAGE_JPEG_VALUE,
            ".jpeg", MediaType.IMAGE_JPEG_VALUE,
            ".png", MediaType.IMAGE_PNG_VALUE,
            ".gif", MediaType.IMAGE_GIF_VALUE,
            ".webp", "image/webp",
            ".pdf", MediaType.APPLICATION_PDF_VALUE
    );

    @Operation(summary = "访问本地文件")
    @GetMapping("/**")
    public void serveFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 提取子路径: /local-files/qrcode/2025/01/01/xxx.jpg → qrcode/2025/01/01/xxx.jpg
        String prefix = request.getContextPath() + "/local-files/";
        String requestUri = request.getRequestURI();
        String relativePath = requestUri.substring(requestUri.indexOf(prefix) + prefix.length());

        // 安全校验：禁止路径穿越
        if (relativePath.contains("..") || relativePath.contains("\\")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "非法路径");
            return;
        }

        Path filePath = Paths.get(localStorageDir, relativePath).normalize();
        // 二次校验：确保解析后的路径仍在 localStorageDir 内
        if (!filePath.startsWith(Paths.get(localStorageDir).normalize())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "禁止访问");
            return;
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        // 根据扩展名设置 Content-Type
        String fileName = filePath.getFileName().toString().toLowerCase();
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";
        String contentType = CONTENT_TYPE_MAP.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(filePath));
        // 缓存 1 小时
        response.setHeader("Cache-Control", "public, max-age=3600");

        try (OutputStream os = response.getOutputStream()) {
            Files.copy(filePath, os);
            os.flush();
        }
    }
}
