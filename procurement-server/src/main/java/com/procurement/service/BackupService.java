package com.procurement.service;

import com.procurement.entity.SysBackup;

import java.util.List;

/**
 * 数据备份服务接口
 */
public interface BackupService {

    /**
     * 创建备份
     */
    SysBackup create(Long enterpriseId, String backupType);

    /**
     * 获取备份历史列表
     */
    List<SysBackup> list(Long enterpriseId);

    /**
     * 从备份恢复
     */
    void restore(Long enterpriseId, Long backupId);
}
