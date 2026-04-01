package com.procurement.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商价格表 Excel 数据模型（按列索引读取）
 *
 * 供应商 Excel 格式（第1行标题，第2行表头，第3行起数据）：
 * A(0):序号 | B(1):产品类别 | C(2):图片(跳过) | D(3):二维码(跳过) | E(4):产品名称
 * F(5):单价 | G(6):销售单位 | H(7):含量 | I(8):单位 | J(9):箱价(进价) | K(10):箱价(售价)
 */
@Data
public class SupplierImportData {

    @ExcelProperty(index = 1)
    private String categoryName;

    @ExcelProperty(index = 4)
    private String name;

    @ExcelProperty(index = 7)
    private String spec;

    @ExcelProperty(index = 8)
    private String unit;

    @ExcelProperty(index = 9)
    private BigDecimal costPrice;

    @ExcelProperty(index = 10)
    private BigDecimal price;
}
