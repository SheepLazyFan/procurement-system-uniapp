package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.SalesOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.SalesOrderResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 销售订单控制器
 */
@Tag(name = "销售订单")
@RestController
@RequestMapping("/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    /** SELLER(memberRole=null) 和 ADMIN 才能看到财务数据 */
    private boolean hasFinancialAccess(LoginUser loginUser) {
        String role = loginUser.getMemberRole();
        return role == null || "ADMIN".equals(role);
    }

    private void maskFinancials(SalesOrderResponse o) {
        o.setTotalCost(null);
        o.setTotalProfit(null);
        if (o.getItems() != null) {
            o.getItems().forEach(i -> { i.setCostPrice(null); i.setProfit(null); });
        }
    }

    @Operation(summary = "订单列表（分页）")
    @GetMapping
    public R<PageResponse<SalesOrderResponse>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String sortBy) {
        PageResponse<SalesOrderResponse> result = salesOrderService.listByPage(loginUser.getEnterpriseId(),
                pageNum, pageSize, status, paymentStatus, customerId, keyword,
                startDate, endDate, minAmount, maxAmount, sortBy);
        if (!hasFinancialAccess(loginUser)) {
            result.getRecords().forEach(this::maskFinancials);
        }
        return R.ok(result);
    }

    @Operation(summary = "各状态订单数量")
    @GetMapping("/count-by-status")
    public R<Map<String, Long>> countByStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentStatus) {
        return R.ok(salesOrderService.countByStatus(loginUser.getEnterpriseId(),
                keyword, startDate, endDate, minAmount, maxAmount, customerId, paymentStatus));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<SalesOrderResponse> getById(@AuthenticationPrincipal LoginUser loginUser,
                                          @PathVariable Long id) {
        SalesOrderResponse result = salesOrderService.getById(loginUser.getEnterpriseId(), id);
        if (!hasFinancialAccess(loginUser)) {
            maskFinancials(result);
        }
        return R.ok(result);
    }

    @Operation(summary = "商家开单")
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SALES')")
    public R<SalesOrderResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody SalesOrderRequest request) {
        return R.ok(salesOrderService.create(loginUser.getEnterpriseId(), request));
    }

    @Operation(summary = "确认订单")
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SALES')")
    public R<Void> confirm(@AuthenticationPrincipal LoginUser loginUser,
                            @PathVariable Long id) {
        salesOrderService.confirm(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "标记发货")
    @PutMapping("/{id}/ship")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SALES', 'WAREHOUSE')")
    public R<Void> ship(@AuthenticationPrincipal LoginUser loginUser,
                         @PathVariable Long id) {
        salesOrderService.ship(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "完成订单（自动扣减库存）")
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SALES', 'WAREHOUSE')")
    public R<Void> complete(@AuthenticationPrincipal LoginUser loginUser,
                             @PathVariable Long id) {
        salesOrderService.complete(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SALES')")
    public R<Void> cancel(@AuthenticationPrincipal LoginUser loginUser,
                            @PathVariable Long id) {
        salesOrderService.cancel(loginUser.getEnterpriseId(), id, loginUser.getUserId(), loginUser.getMemberRole());
        return R.ok();
    }

    @Operation(summary = "确认收款（商家/销售确认线下已收款）")
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SALES')")
    public R<Void> confirmPayment(@AuthenticationPrincipal LoginUser loginUser,
                                   @PathVariable Long id) {
        salesOrderService.confirmPayment(loginUser.getEnterpriseId(), id, loginUser.getUserId(), loginUser.getMemberRole());
        return R.ok();
    }
}
