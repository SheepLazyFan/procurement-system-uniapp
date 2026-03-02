package com.procurement.service;

import com.procurement.dto.request.BuyerOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductResponse;
import com.procurement.dto.response.SalesOrderResponse;

import java.util.List;
import java.util.Map;

/**
 * 买家端服务接口
 */
public interface BuyerService {

    /**
     * 获取商家门店信息
     */
    Map<String, Object> getStoreInfo(Long enterpriseId);

    /**
     * 获取商家分类列表
     */
    List<Map<String, Object>> getStoreCategories(Long enterpriseId);

    /**
     * 获取商家商品列表（不含 costPrice）
     */
    PageResponse<ProductResponse> getStoreProducts(Long enterpriseId, Long categoryId,
                                                    String keyword, Integer pageNum, Integer pageSize);

    /**
     * 获取商品详情（不含 costPrice）
     */
    Map<String, Object> getProductDetail(Long productId);

    /**
     * 买家提交采购订单
     */
    SalesOrderResponse createOrder(Long buyerUserId, BuyerOrderRequest request);

    /**
     * 买家伪支付
     */
    void payOrder(Long buyerUserId, Long orderId);

    /**
     * 买家订单列表
     */
    PageResponse<SalesOrderResponse> listOrders(Long buyerUserId, Integer pageNum,
                                                 Integer pageSize, String status);

    /**
     * 买家订单详情
     */
    SalesOrderResponse getOrderDetail(Long buyerUserId, Long orderId);
}
