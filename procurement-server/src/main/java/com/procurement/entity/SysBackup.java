package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据备份表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_backup")
public class SysBackup extends BaseEntity {

    /** 所属企业 */
    private Long enterpriseId;

    /** COS 备份文件 URL */
    private String fileUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 备份类型：FULL/PARTIAL */
    private String backupType;

    /** 状态：PROCESSING/COMPLETED/FAILED */
    private String status;

    /** 备注 */
    private String remark;
}
