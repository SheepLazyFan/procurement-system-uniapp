package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.PurchaseOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.PurchaseOrderResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.PurchaseOrderService;
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
 * 采购订单控制器
 */
@Tag(name = "采购订单")
@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'WAREHOUSE')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @Operation(summary = "订单列表（分页）")
    @GetMapping
    public R<PageResponse<PurchaseOrderResponse>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String sortBy) {
        return R.ok(purchaseOrderService.listByPage(loginUser.getEnterpriseId(),
                pageNum, pageSize, status, supplierId, keyword, startDate, endDate, minAmount, maxAmount, sortBy));
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
            @RequestParam(required = false) Long supplierId) {
        return R.ok(purchaseOrderService.countByStatus(loginUser.getEnterpriseId(),
                keyword, startDate, endDate, minAmount, maxAmount, supplierId));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<PurchaseOrderResponse> getById(@AuthenticationPrincipal LoginUser loginUser,
                                             @PathVariable Long id) {
        return R.ok(purchaseOrderService.getById(loginUser.getEnterpriseId(), id));
    }

    @Operation(summary = "创建采购订单（快速采购）")
    @PostMapping
    public R<PurchaseOrderResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody PurchaseOrderRequest request) {
        return R.ok(purchaseOrderService.create(loginUser.getEnterpriseId(), request));
    }

    @Operation(summary = "标记采购中")
    @PutMapping("/{id}/purchasing")
    public R<Void> purchasing(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable Long id) {
        purchaseOrderService.purchasing(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "标记到货（自动增加库存）")
    @PutMapping("/{id}/arrive")
    public R<Void> arrive(@AuthenticationPrincipal LoginUser loginUser,
                           @PathVariable Long id) {
        purchaseOrderService.arrive(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "完成采购")
    @PutMapping("/{id}/complete")
    public R<Void> complete(@AuthenticationPrincipal LoginUser loginUser,
                             @PathVariable Long id) {
        purchaseOrderService.complete(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "取消采购")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@AuthenticationPrincipal LoginUser loginUser,
                            @PathVariable Long id) {
        purchaseOrderService.cancel(loginUser.getEnterpriseId(), id);
        return R.ok();
    }
}
