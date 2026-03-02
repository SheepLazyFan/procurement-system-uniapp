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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 销售订单控制器
 */
@Tag(name = "销售订单")
@RestController
@RequestMapping("/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @Operation(summary = "订单列表（分页）")
    @GetMapping
    public R<PageResponse<SalesOrderResponse>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.ok(salesOrderService.listByPage(loginUser.getEnterpriseId(),
                pageNum, pageSize, status, paymentStatus, customerId, startDate, endDate));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<SalesOrderResponse> getById(@AuthenticationPrincipal LoginUser loginUser,
                                          @PathVariable Long id) {
        return R.ok(salesOrderService.getById(loginUser.getEnterpriseId(), id));
    }

    @Operation(summary = "商家开单")
    @PostMapping
    public R<SalesOrderResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody SalesOrderRequest request) {
        return R.ok(salesOrderService.create(loginUser.getEnterpriseId(), request));
    }

    @Operation(summary = "确认订单")
    @PutMapping("/{id}/confirm")
    public R<Void> confirm(@AuthenticationPrincipal LoginUser loginUser,
                            @PathVariable Long id) {
        salesOrderService.confirm(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "标记发货")
    @PutMapping("/{id}/ship")
    public R<Void> ship(@AuthenticationPrincipal LoginUser loginUser,
                         @PathVariable Long id) {
        salesOrderService.ship(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "完成订单（自动扣减库存）")
    @PutMapping("/{id}/complete")
    public R<Void> complete(@AuthenticationPrincipal LoginUser loginUser,
                             @PathVariable Long id) {
        salesOrderService.complete(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@AuthenticationPrincipal LoginUser loginUser,
                            @PathVariable Long id) {
        salesOrderService.cancel(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "标记已支付（伪支付）")
    @PutMapping("/{id}/pay")
    public R<Void> pay(@AuthenticationPrincipal LoginUser loginUser,
                        @PathVariable Long id) {
        salesOrderService.pay(loginUser.getEnterpriseId(), id);
        return R.ok();
    }
}
