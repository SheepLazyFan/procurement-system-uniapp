package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "pms_product", autoResultMap = true)
public class PmsProduct extends BaseEntity {

    /** 所属企业 */
    private Long enterpriseId;

    /** 所属分类 */
    private Long categoryId;

    /** 商品名称 */
    private String name;

    /** 规格型号 */
    private String spec;

    /** 计量单位 */
    private String unit;

    /** 销售单价 */
    private BigDecimal price;

    /** 成本价 */
    private BigDecimal costPrice;

    /** 当前库存量 */
    private Integer stock;

    /** 库存预警阈值 */
    private Integer stockWarning;

    /** 商品图片 URL 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /** 二维码图片 URL（扫码查看演示视频等）— 当前本地存储 data/image/，部署后迁移 COS */
    private String qrcodeImage;

    /** 商品描述（图文介绍） */
    private String description;

    /** 状态：1=上架，0=下架 */
    private Integer status;
}
