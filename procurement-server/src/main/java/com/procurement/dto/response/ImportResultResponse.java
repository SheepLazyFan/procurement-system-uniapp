package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 批量导入结果响应 DTO
 */
@Data
public class ImportResultResponse implements Serializable {

    /** 总行数 */
    private Integer totalCount;

    /** 成功导入行数 */
    private Integer successCount;

    /** 失败行数 */
    private Integer failCount;

    /** 新建分类数 */
    private Integer newCategoryCount;

    /** 失败详情列表 */
    private List<Map<String, Object>> errors;
}
