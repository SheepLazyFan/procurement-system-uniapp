package com.procurement.service;

import com.procurement.dto.request.CustomerRequest;
import com.procurement.dto.response.CustomerResponse;
import com.procurement.dto.response.PageResponse;

/**
 * 客户管理服务接口
 */
public interface CustomerService {

    /**
     * 分页查询客户列表
     */
    PageResponse<CustomerResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize, String keyword);

    /**
     * 获取客户详情
     */
    CustomerResponse getById(Long enterpriseId, Long id);

    /**
     * 创建客户
     */
    CustomerResponse create(Long enterpriseId, CustomerRequest request);

    /**
     * 更新客户
     */
    CustomerResponse update(Long enterpriseId, Long id, CustomerRequest request);

    /**
     * 删除客户
     */
    void delete(Long enterpriseId, Long id);
}
