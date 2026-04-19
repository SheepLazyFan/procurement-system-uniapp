package com.procurement.service;

import com.procurement.dto.request.BuyerOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductResponse;
import com.procurement.dto.response.SalesOrderResponse;

import java.math.BigDecimal;
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
     * 获取商家分类列表（各分类数量与传入筛选条件同步）
     */
    List<Map<String, Object>> getStoreCategories(Long enterpriseId, String stockStatus,
                                                   BigDecimal priceMin, BigDecimal priceMax);

    /**
     * 获取商家商品列表（不含 costPrice）
     *
     * @param stockStatus 库存状态筛选：IN_STOCK / OUT_OF_STOCK / null(全部)
     * @param sortBy      排序方式：price_asc / price_desc / null(默认按ID倒序)
     * @param priceMin    最低售价（含）
     * @param priceMax    最高售价（含）
     */
    PageResponse<ProductResponse> getStoreProducts(Long enterpriseId, Long categoryId,
                                                    String keyword,
                                                    String stockStatus, String sortBy,
                                                    BigDecimal priceMin, BigDecimal priceMax,
                                                    Integer pageNum, Integer pageSize);

    /**
     * 获取商品详情（不含 costPrice）
     */
    Map<String, Object> getProductDetail(Long productId);

    /**
     * 买家提交采购订单
     */
    SalesOrderResponse createOrder(Long buyerUserId, BuyerOrderRequest request);

    /**
     * 兼容旧版买家 pay 接口：降级为付款声明（UNPAID -> CLAIMED）
     */
    void payOrder(Long buyerUserId, Long orderId);

    /** 买家声明已完成线下付款（UNPAID → CLAIMED） */
    void claimPaid(Long buyerUserId, Long orderId);

    /**
     * 买家订单列表
     */
    PageResponse<SalesOrderResponse> listOrders(Long buyerUserId, Integer pageNum,
                                                 Integer pageSize, String status);

    /**
     * 买家订单详情
     */
    SalesOrderResponse getOrderDetail(Long buyerUserId, Long orderId);

    /**
     * 买家取消订单
     */
    void cancelOrder(Long buyerUserId, Long orderId);
}
