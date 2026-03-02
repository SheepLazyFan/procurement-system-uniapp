package com.procurement.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 批量导入商品请求 DTO
 */
@Data
public class BatchImportRequest implements Serializable {

    @NotEmpty(message = "导入数据不能为空")
    @Size(max = 500, message = "单次最多导入500条")
    private List<ImportItem> items;

    /** 重复策略：SKIP(跳过) / OVERWRITE(覆盖) */
    @Pattern(regexp = "^(SKIP|OVERWRITE)$", message = "重复策略只能是 SKIP 或 OVERWRITE")
    private String duplicateStrategy = "SKIP";

    @Data
    public static class ImportItem implements Serializable {
        private String categoryName;
        private String name;
        private String spec;
        private String unit;
        private BigDecimal price;
        private BigDecimal costPrice;
        private Integer stock;
    }
}
