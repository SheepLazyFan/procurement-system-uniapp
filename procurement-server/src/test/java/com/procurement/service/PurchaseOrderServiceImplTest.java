package com.procurement.service;

import com.procurement.common.constant.OrderConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.entity.OmsPurchaseOrder;
import com.procurement.entity.OmsPurchaseOrderItem;
import com.procurement.entity.PmsProduct;
import com.procurement.mapper.*;
import com.procurement.service.impl.PurchaseOrderServiceImpl;
import com.procurement.common.util.OrderNoGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PurchaseOrderServiceImpl 单元测试
 * <p>
 * 覆盖：创建、状态流转（purchasing/arrive/complete/cancel）、BUG-2 修复验证。
 * 依赖全部 Mock，不启动 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderServiceImpl — 采购订单服务")
class PurchaseOrderServiceImplTest {

    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock private ProductMapper productMapper;
    @Mock private SupplierMapper supplierMapper;
    @Mock private ProductSupplierMapper productSupplierMapper;
    @Mock private OrderNoGenerator orderNoGenerator;
    @Mock private StockWarningNotificationService notificationService;

    @InjectMocks
    private PurchaseOrderServiceImpl purchaseOrderService;

    // ===========================================================
    // 辅助方法
    // ===========================================================

    private OmsPurchaseOrder buildOrder(Long id, Long enterpriseId, String status) {
        OmsPurchaseOrder o = new OmsPurchaseOrder();
        o.setId(id);
        o.setEnterpriseId(enterpriseId);
        o.setStatus(status);
        o.setSupplierId(1L);
        o.setTotalAmount(BigDecimal.ZERO);
        return o;
    }

    // ===========================================================
    // 1. 状态流转 — purchasing
    // ===========================================================

    @Nested
    @DisplayName("purchasing — DRAFT → PURCHASING")
    class PurchasingTests {

        @Test
        @DisplayName("Should transition to PURCHASING when status is DRAFT")
        void should_purchasing_when_draft() {
            OmsPurchaseOrder order = buildOrder(1L, 1L, OrderConstants.PURCHASE_DRAFT);
            when(purchaseOrderMapper.selectById(1L)).thenReturn(order);

            purchaseOrderService.purchasing(1L, 1L);

            verify(purchaseOrderMapper).updateById(argThat(o ->
                    OrderConstants.PURCHASE_PURCHASING.equals(o.getStatus())));
        }

        @Test
        @DisplayName("Should throw ORDER_STATUS_ERROR when purchasing from ARRIVED")
        void should_throwError_when_purchasingFromArrived() {
            OmsPurchaseOrder order = buildOrder(2L, 1L, OrderConstants.PURCHASE_ARRIVED);
            when(purchaseOrderMapper.selectById(2L)).thenReturn(order);

            assertThatThrownBy(() -> purchaseOrderService.purchasing(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
        }
    }

    // ===========================================================
    // 2. 状态流转 — arrive (PURCHASING → ARRIVED)
    // ===========================================================

    @Nested
    @DisplayName("arrive — PURCHASING → ARRIVED + 库存入库")
    class ArriveTests {

        @Test
        @DisplayName("Should transition to ARRIVED and increase product stock")
        void should_arriveAndIncreaseStock() {
            OmsPurchaseOrder order = buildOrder(3L, 1L, OrderConstants.PURCHASE_PURCHASING);
            when(purchaseOrderMapper.selectById(3L)).thenReturn(order);

            OmsPurchaseOrderItem item = new OmsPurchaseOrderItem();
            item.setProductId(10L);
            item.setQuantity(5);
            when(purchaseOrderItemMapper.selectList(any())).thenReturn(List.of(item));

            // arrive() 调用 adjustStock 增加库存，而非 selectById
            when(productMapper.adjustStock(10L, 5)).thenReturn(1);

            purchaseOrderService.arrive(1L, 3L);

            // 验证库存增加
            verify(productMapper).adjustStock(10L, 5);
            // 验证状态变更
            verify(purchaseOrderMapper).updateById(argThat(o ->
                    OrderConstants.PURCHASE_ARRIVED.equals(o.getStatus())));
        }
    }

    // ===========================================================
    // 3. 状态流转 — complete (ARRIVED → COMPLETED)
    // ===========================================================

    @Nested
    @DisplayName("complete — ARRIVED → COMPLETED")
    class CompleteTests {

        @Test
        @DisplayName("Should transition to COMPLETED when status is ARRIVED")
        void should_complete_when_arrived() {
            OmsPurchaseOrder order = buildOrder(4L, 1L, OrderConstants.PURCHASE_ARRIVED);
            when(purchaseOrderMapper.selectById(4L)).thenReturn(order);

            purchaseOrderService.complete(1L, 4L);

            verify(purchaseOrderMapper).updateById(argThat(o ->
                    OrderConstants.PURCHASE_COMPLETED.equals(o.getStatus())));
        }

        @Test
        @DisplayName("Should throw ORDER_STATUS_ERROR when completing DRAFT order")
        void should_throwError_when_completingDraft() {
            OmsPurchaseOrder order = buildOrder(5L, 1L, OrderConstants.PURCHASE_DRAFT);
            when(purchaseOrderMapper.selectById(5L)).thenReturn(order);

            assertThatThrownBy(() -> purchaseOrderService.complete(1L, 5L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ===========================================================
    // 4. cancel — BUG-2 修复验证
    // ===========================================================

    @Nested
    @DisplayName("cancel — BUG-2: 仅 DRAFT/PURCHASING 可取消")
    class CancelTests {

        @Test
        @DisplayName("Should cancel DRAFT order successfully")
        void should_cancelDraft() {
            OmsPurchaseOrder order = buildOrder(6L, 1L, OrderConstants.PURCHASE_DRAFT);
            when(purchaseOrderMapper.selectById(6L)).thenReturn(order);

            purchaseOrderService.cancel(1L, 6L);

            verify(purchaseOrderMapper).updateById(argThat(o ->
                    OrderConstants.PURCHASE_CANCELLED.equals(o.getStatus())));
        }

        @Test
        @DisplayName("Should cancel PURCHASING order successfully")
        void should_cancelPurchasing() {
            OmsPurchaseOrder order = buildOrder(7L, 1L, OrderConstants.PURCHASE_PURCHASING);
            when(purchaseOrderMapper.selectById(7L)).thenReturn(order);

            purchaseOrderService.cancel(1L, 7L);

            verify(purchaseOrderMapper).updateById(argThat(o ->
                    OrderConstants.PURCHASE_CANCELLED.equals(o.getStatus())));
        }

        @Test
        @DisplayName("Should throw ORDER_STATUS_ERROR when cancelling ARRIVED order (BUG-2 fix)")
        void should_throwError_when_cancellingArrived() {
            OmsPurchaseOrder order = buildOrder(8L, 1L, OrderConstants.PURCHASE_ARRIVED);
            when(purchaseOrderMapper.selectById(8L)).thenReturn(order);

            assertThatThrownBy(() -> purchaseOrderService.cancel(1L, 8L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
        }

        @Test
        @DisplayName("Should throw ORDER_STATUS_ERROR when cancelling COMPLETED order")
        void should_throwError_when_cancellingCompleted() {
            OmsPurchaseOrder order = buildOrder(9L, 1L, OrderConstants.PURCHASE_COMPLETED);
            when(purchaseOrderMapper.selectById(9L)).thenReturn(order);

            assertThatThrownBy(() -> purchaseOrderService.cancel(1L, 9L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
        }

        @Test
        @DisplayName("Should throw ORDER_STATUS_ERROR when cancelling already CANCELLED order")
        void should_throwError_when_cancellingCancelled() {
            OmsPurchaseOrder order = buildOrder(10L, 1L, OrderConstants.PURCHASE_CANCELLED);
            when(purchaseOrderMapper.selectById(10L)).thenReturn(order);

            assertThatThrownBy(() -> purchaseOrderService.cancel(1L, 10L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should NOT adjust stock when cancelling DRAFT (no stock was ever added)")
        void should_notAdjustStock_when_cancellingDraft() {
            OmsPurchaseOrder order = buildOrder(11L, 1L, OrderConstants.PURCHASE_DRAFT);
            when(purchaseOrderMapper.selectById(11L)).thenReturn(order);

            purchaseOrderService.cancel(1L, 11L);

            // DRAFT 取消不涉及库存
            verify(productMapper, never()).adjustStock(any(), anyInt());
        }
    }

    // ===========================================================
    // 5. 企业数据隔离
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when accessing another enterprise's order")
    void should_throwNotFound_when_accessingOtherEnterpriseOrder() {
        OmsPurchaseOrder order = buildOrder(12L, 999L, OrderConstants.PURCHASE_DRAFT);
        when(purchaseOrderMapper.selectById(12L)).thenReturn(order);

        assertThatThrownBy(() -> purchaseOrderService.cancel(1L, 12L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }
}
