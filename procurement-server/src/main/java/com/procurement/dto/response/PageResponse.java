package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页响应 DTO
 */
@Data
public class PageResponse<T> implements Serializable {

    /** 数据列表 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;

    public static <T> PageResponse<T> of(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        PageResponse<T> response = new PageResponse<>();
        response.setRecords(records);
        response.setTotal(total);
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        return response;
    }
}
