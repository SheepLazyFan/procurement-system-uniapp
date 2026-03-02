package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 批量导入记录表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_import_log", autoResultMap = true)
public class SysImportLog extends BaseEntity {

    /** 所属企业 */
    private Long enterpriseId;

    /** 操作用户 */
    private Long userId;

    /** 上传的文件名 */
    private String fileName;

    /** COS 存档 URL */
    private String fileUrl;

    /** 总行数 */
    private Integer totalCount;

    /** 成功导入行数 */
    private Integer successCount;

    /** 失败行数 */
    private Integer failCount;

    /** 新建分类数 */
    private Integer newCategoryCount;

    /** 状态：PROCESSING/COMPLETED/FAILED */
    private String status;

    /** 失败详情 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> errorDetail;
}
