package com.procurement.service;

import com.procurement.dto.request.SalesOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.SalesOrderResponse;

/**
 * 销售订单服务接口
 */
public interface SalesOrderService {

    /**
     * 分页查询销售订单
     */
    PageResponse<SalesOrderResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                                 String status, String paymentStatus,
                                                 Long customerId, String startDate, String endDate);

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
    void cancel(Long enterpriseId, Long id);

    /**
     * 标记已支付（伪支付）
     */
    void pay(Long enterpriseId, Long id);
}
