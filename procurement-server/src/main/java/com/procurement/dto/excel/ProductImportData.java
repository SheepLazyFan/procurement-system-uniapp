package com.procurement.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import com.alibaba.excel.enums.poi.FillPatternTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品批量导入 Excel 数据模型（EasyExcel）
 *
 * 列头格式样板：
 * | 商品分类(必填) | 商品名称(必填) | 规格型号 | 计量单位(必填) | 销售单价(必填) | 成本价 | 初始库存 | 库存预警阈值 | 二维码图片URL |
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@HeadStyle(fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND, fillForegroundColor = 22)
public class ProductImportData {

    @ExcelProperty("商品分类(必填)")
    @ColumnWidth(18)
    private String categoryName;

    @ExcelProperty("商品名称(必填)")
    @ColumnWidth(25)
    private String name;

    @ExcelProperty("规格型号")
    @ColumnWidth(20)
    private String spec;

    @ExcelProperty("计量单位(必填)")
    @ColumnWidth(14)
    private String unit;

    @ExcelProperty("销售单价(必填)")
    @ColumnWidth(14)
    private BigDecimal price;

    @ExcelProperty("成本价")
    @ColumnWidth(12)
    private BigDecimal costPrice;

    @ExcelProperty("初始库存")
    @ColumnWidth(12)
    private Integer stock;

    @ExcelProperty("库存预警阈值")
    @ColumnWidth(14)
    private Integer stockWarning;

    /** 二维码图片 URL（可选） — TODO: 后续迁移 COS 后 URL 前缀会变 */
    @ExcelProperty("二维码图片URL")
    @ColumnWidth(40)
    private String qrcodeImageUrl;
}
