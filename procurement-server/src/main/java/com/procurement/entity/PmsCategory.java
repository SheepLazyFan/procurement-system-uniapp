package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_category")
public class PmsCategory extends BaseEntity {

    /** 所属企业 */
    private Long enterpriseId;

    /** 分类名称 */
    private String name;

    /** 排序值（升序） */
    private Integer sortOrder;
}
