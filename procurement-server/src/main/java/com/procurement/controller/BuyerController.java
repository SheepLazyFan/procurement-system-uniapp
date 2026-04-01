package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.BuyerOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductResponse;
import com.procurement.dto.response.SalesOrderResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.BuyerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 买家端控制器 — 商家库存浏览 + 下单
 */
@Tag(name = "买家端")
@RestController
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerController {

    private final BuyerService buyerService;

    @Operation(summary = "获取商家门店信息")
    @GetMapping("/store/{enterpriseId}")
    public R<Map<String, Object>> getStoreInfo(@PathVariable Long enterpriseId) {
        return R.ok(buyerService.getStoreInfo(enterpriseId));
    }

    @Operation(summary = "商家分类列表")
    @GetMapping("/store/{enterpriseId}/categories")
    public R<List<Map<String, Object>>> getStoreCategories(
            @PathVariable Long enterpriseId,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax) {
        return R.ok(buyerService.getStoreCategories(enterpriseId, stockStatus, priceMin, priceMax));
    }

    @Operation(summary = "商家商品列表")
    @GetMapping("/store/{enterpriseId}/products")
    public R<PageResponse<ProductResponse>> getStoreProducts(
            @PathVariable Long enterpriseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(buyerService.getStoreProducts(
                enterpriseId, categoryId, keyword,
                stockStatus, sortBy, priceMin, priceMax,
                pageNum, pageSize));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/product/{id}")
    public R<Map<String, Object>> getProductDetail(@PathVariable Long id) {
        return R.ok(buyerService.getProductDetail(id));
    }

    @Operation(summary = "提交采购订单")
    @PostMapping("/orders")
    public R<SalesOrderResponse> createOrder(@AuthenticationPrincipal LoginUser loginUser,
                                              @Valid @RequestBody BuyerOrderRequest request) {
        return R.ok(buyerService.createOrder(loginUser.getUserId(), request));
    }

    @Operation(summary = "伪支付")
    @PutMapping("/orders/{id}/pay")
    public R<Void> payOrder(@AuthenticationPrincipal LoginUser loginUser,
                             @PathVariable Long id) {
        buyerService.payOrder(loginUser.getUserId(), id);
        return R.ok();
    }

    @Operation(summary = "买家声明线下已付款（UNPAID → CLAIMED）")
    @PutMapping("/orders/{id}/claim-paid")
    public R<Void> claimPaid(@AuthenticationPrincipal LoginUser loginUser,
                              @PathVariable Long id) {
        buyerService.claimPaid(loginUser.getUserId(), id);
        return R.ok();
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/orders")
    public R<PageResponse<SalesOrderResponse>> listOrders(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status) {
        return R.ok(buyerService.listOrders(loginUser.getUserId(), pageNum, pageSize, status));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public R<SalesOrderResponse> getOrderDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                 @PathVariable Long id) {
        return R.ok(buyerService.getOrderDetail(loginUser.getUserId(), id));
    }

    @Operation(summary = "买家取消订单")
    @PutMapping("/orders/{id}/cancel")
    public R<Void> cancelOrder(@AuthenticationPrincipal LoginUser loginUser,
                                @PathVariable Long id) {
        buyerService.cancelOrder(loginUser.getUserId(), id);
        return R.ok();
    }
}
