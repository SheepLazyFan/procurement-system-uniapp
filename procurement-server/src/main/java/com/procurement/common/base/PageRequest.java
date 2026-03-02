package com.procurement.common.base;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求基类
 */
@Data
public class PageRequest implements Serializable {

    /** 当前页码（默认第1页） */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /** 每页条数（默认20条，最大100条） */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 20;
}
