package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.entity.SysBackup;
import com.procurement.mapper.BackupMapper;
import com.procurement.service.BackupService;
import com.procurement.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据备份服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final BackupMapper backupMapper;

    @Override
    @Transactional
    public SysBackup create(Long enterpriseId, String backupType) {
        SysBackup backup = new SysBackup();
        backup.setEnterpriseId(enterpriseId);
        backup.setBackupType(backupType != null ? backupType : "FULL");
        backup.setStatus("PROCESSING");
        backup.setRemark("数据备份 - " + backupType);
        backupMapper.insert(backup);

        // TODO: 实际备份业务 — 导出数据到 JSON/Excel 并上传 COS
        // 临时标记为完成
        backup.setFileUrl("https://cos.example.com/backup/" + backup.getId() + ".json");
        backup.setFileSize(0L);
        backup.setStatus("COMPLETED");
        backupMapper.updateById(backup);

        log.info("备份创建成功，enterpriseId={}, backupId={}", enterpriseId, backup.getId());
        return backup;
    }

    @Override
    public List<SysBackup> list(Long enterpriseId) {
        return backupMapper.selectList(
                new LambdaQueryWrapper<SysBackup>()
                        .eq(SysBackup::getEnterpriseId, enterpriseId)
                        .orderByDesc(SysBackup::getId));
    }

    @Override
    @Transactional
    public void restore(Long enterpriseId, Long backupId) {
        SysBackup backup = backupMapper.selectById(backupId);
        if (backup == null || !backup.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (!"COMPLETED".equals(backup.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "备份状态异常，无法恢复");
        }

        // TODO: 实际恢复业务 — 从 COS 下载备份文件并恢复数据
        log.info("数据恢复已触发（异步执行），backupId={}", backupId);
    }
}
