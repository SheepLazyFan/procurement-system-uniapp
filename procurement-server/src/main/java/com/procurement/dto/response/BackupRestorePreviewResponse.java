package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 备份恢复预检信息
 */
@Data
public class BackupRestorePreviewResponse implements Serializable {

    private Long backupId;
    private String backupFileName;
    private String backupType;
    private String backupCreatedAt;
    private Long enterpriseId;
    private String currentEnterpriseName;
    private String backupEnterpriseName;
    private boolean willCreatePreRestoreSnapshot;
    private boolean willForceRelogin;
    private Map<String, Integer> recordCounts = new LinkedHashMap<>();
    private List<String> warnings;
}
