package com.procurement.service;

import com.procurement.dto.response.BackupRestorePreviewResponse;
import com.procurement.entity.SysBackup;

import java.io.File;
import java.util.List;

/**
 * 数据备份服务接口
 */
public interface BackupService {

    /**
     * 创建备份（JSON 全量导出到本地 backup 目录）
     */
    SysBackup create(Long enterpriseId, String backupType);

    /**
     * 获取备份历史列表
     */
    List<SysBackup> list(Long enterpriseId);

    /**
     * 恢复预检信息
     */
    BackupRestorePreviewResponse previewRestore(Long enterpriseId, Long backupId);

    /**
     * 从备份恢复（全量覆盖当前企业数据）
     */
    void restore(Long enterpriseId, Long backupId, Long operatorUserId);

    /**
     * 获取备份文件（用于下载）
     */
    File getBackupFile(Long enterpriseId, Long backupId);

    /**
     * 删除备份（记录 + 文件）
     */
    void delete(Long enterpriseId, Long backupId);
}
