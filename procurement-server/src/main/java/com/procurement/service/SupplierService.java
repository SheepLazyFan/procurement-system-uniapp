package com.procurement.service;

import com.procurement.dto.request.SupplierRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.SupplierResponse;

/**
 * 供应商管理服务接口
 */
public interface SupplierService {

    /**
     * 分页查询供应商列表
     */
    PageResponse<SupplierResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize, String keyword);

    /**
     * 获取供应商详情
     */
    SupplierResponse getById(Long enterpriseId, Long id);

    /**
     * 创建供应商
     */
    SupplierResponse create(Long enterpriseId, SupplierRequest request);

    /**
     * 更新供应商
     */
    SupplierResponse update(Long enterpriseId, Long id, SupplierRequest request);

    /**
     * 删除供应商
     */
    void delete(Long enterpriseId, Long id);
}
