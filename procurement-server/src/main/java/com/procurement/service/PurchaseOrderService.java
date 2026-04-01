package com.procurement.service;

import com.procurement.dto.request.PurchaseOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.PurchaseOrderResponse;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 采购订单服务接口
 */
public interface PurchaseOrderService {

    /**
     * 分页查询采购订单
     */
    PageResponse<PurchaseOrderResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                                    String status, Long supplierId,
                                                    String keyword, String startDate, String endDate,
                                                    BigDecimal minAmount, BigDecimal maxAmount, String sortBy);

    /**
     * 各状态订单数量统计（支持筛选条件）
     */
    Map<String, Long> countByStatus(Long enterpriseId, String keyword, String startDate, String endDate,
                                     BigDecimal minAmount, BigDecimal maxAmount, Long supplierId);

    /**
     * 获取订单详情
     */
    PurchaseOrderResponse getById(Long enterpriseId, Long id);

    /**
     * 创建采购订单（快速采购）
     */
    PurchaseOrderResponse create(Long enterpriseId, PurchaseOrderRequest request);

    /**
     * 标记采购中
     */
    void purchasing(Long enterpriseId, Long id);

    /**
     * 标记到货（自动增加库存）
     */
    void arrive(Long enterpriseId, Long id);

    /**
     * 完成采购
     */
    void complete(Long enterpriseId, Long id);

    /**
     * 取消采购
     */
    void cancel(Long enterpriseId, Long id);
}
