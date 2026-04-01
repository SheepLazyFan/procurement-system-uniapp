package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传控制器 — 腾讯云 COS
 */
@Tag(name = "文件上传")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件（商品图片 / Excel）")
    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "type", defaultValue = "image") String type) {
        return R.ok(fileService.upload(file, type));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public R<Void> delete(@RequestParam("url") String url) {
        fileService.delete(url);
        return R.ok();
    }
}
