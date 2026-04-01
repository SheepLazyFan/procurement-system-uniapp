package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品响应 DTO
 */
@Data
public class ProductResponse implements Serializable {
    private Long id;
    private String name;
    private String spec;
    private String unit;
    private BigDecimal price;
    private BigDecimal costPrice;
    /**
     * 供货价：仅当通过供应商筛选（?supplierId=X）查询时才返回，其他场景为 null。
     * 快速采购预填价格优先级：supplyPrice > costPrice > price
     */
    private BigDecimal supplyPrice;
    private Integer stock;
    /** 库存状态（买家接口使用）：IN_STOCK / OUT_OF_STOCK */
    private String stockStatus;
    private Integer stockWarning;
    private List<String> images;
    /** 主图：images[0]，供买家列表卡片展示 */
    private String mainImage;
    /** 二维码图片 URL（扫码查看演示视频）— 当前本地存储，部署后迁移 COS */
    private String qrcodeImage;
    private Long categoryId;
    private String categoryName;
    private Integer status;
    /** 商品描述 */
    private String description;
}
