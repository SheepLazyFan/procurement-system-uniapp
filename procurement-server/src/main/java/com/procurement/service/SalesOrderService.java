package com.procurement.service;

import com.procurement.dto.request.SalesOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.SalesOrderResponse;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 销售订单服务接口
 */
public interface SalesOrderService {

    /**
     * 分页查询销售订单
     */
    PageResponse<SalesOrderResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                                 String status, String paymentStatus,
                                                 Long customerId, String keyword,
                                                 String startDate, String endDate,
                                                 BigDecimal minAmount, BigDecimal maxAmount, String sortBy);

    /**
     * 各状态订单数量（含筛选条件）
     */
    Map<String, Long> countByStatus(Long enterpriseId, String keyword, String startDate, String endDate,
                                     BigDecimal minAmount, BigDecimal maxAmount,
                                     Long customerId, String paymentStatus);

    /**
     * 获取订单详情
     */
    SalesOrderResponse getById(Long enterpriseId, Long id);

    /**
     * 商家开单（创建销售订单）
     */
    SalesOrderResponse create(Long enterpriseId, SalesOrderRequest request);

    /**
     * 确认订单
     */
    void confirm(Long enterpriseId, Long id);

    /**
     * 标记发货
     */
    void ship(Long enterpriseId, Long id);

    /**
     * 完成订单（自动扣减库存）
     */
    void complete(Long enterpriseId, Long id);

    /**
     * 取消订单（已扣减库存则恢复）
     */
    void cancel(Long enterpriseId, Long id, Long operatorUserId, String callerMemberRole);

    /**
     * 确认收款（商家/销售确认线下已收到款项）
     */
    void confirmPayment(Long enterpriseId, Long id, Long operatorUserId, String callerMemberRole);
}
