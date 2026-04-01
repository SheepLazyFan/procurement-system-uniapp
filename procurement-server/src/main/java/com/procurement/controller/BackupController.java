package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.entity.SysBackup;
import com.procurement.security.LoginUser;
import com.procurement.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 数据备份控制器
 */
@Tag(name = "数据备份")
@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class BackupController {

    private final BackupService backupService;

    @Operation(summary = "创建备份")
    @PostMapping
    public R<SysBackup> create(@AuthenticationPrincipal LoginUser loginUser,
                                @RequestBody(required = false) Map<String, String> body) {
        String backupType = body != null ? body.get("backupType") : "FULL";
        return R.ok(backupService.create(loginUser.getEnterpriseId(), backupType));
    }

    @Operation(summary = "备份历史列表")
    @GetMapping("/list")
    public R<List<SysBackup>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(backupService.list(loginUser.getEnterpriseId()));
    }

    @Operation(summary = "从备份恢复")
    @PostMapping("/{id}/restore")
    public R<Void> restore(@AuthenticationPrincipal LoginUser loginUser,
                            @PathVariable Long id) {
        backupService.restore(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "下载备份文件")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal LoginUser loginUser,
                                              @PathVariable Long id) {
        File file = backupService.getBackupFile(loginUser.getEnterpriseId(), id);
        Resource resource = new FileSystemResource(file);
        String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }

    @Operation(summary = "删除备份")
    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                           @PathVariable Long id) {
        backupService.delete(loginUser.getEnterpriseId(), id);
        return R.ok();
    }
}
