package com.procurement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品分类创建/更新请求 DTO
 */
@Data
public class CategoryRequest implements Serializable {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称最长50字符")
    private String name;

    @Min(value = 0, message = "排序值不能为负数")
    private Integer sortOrder = 0;
}
