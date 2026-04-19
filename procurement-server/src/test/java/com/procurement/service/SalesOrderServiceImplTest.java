package com.procurement.service;

import com.procurement.common.constant.OrderConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.SalesOrderRequest;
import com.procurement.dto.response.SalesOrderResponse;
import com.procurement.entity.OmsSalesOrder;
import com.procurement.entity.OmsSalesOrderItem;
import com.procurement.entity.PmsProduct;
import com.procurement.mapper.CustomerMapper;
import com.procurement.mapper.ProductMapper;
import com.procurement.mapper.SalesOrderItemMapper;
import com.procurement.mapper.SalesOrderMapper;
import com.procurement.service.impl.SalesOrderServiceImpl;
import com.procurement.common.util.OrderNoGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SalesOrderServiceImpl \u5355\u5143\u6d4b\u8bd5
 * <p>
 * \u6d4b\u8bd5\u8303\u56f4\uff1a\u8ba2\u5355\u521b\u5efa\u3001\u91d1\u989d\u5feb\u7167\u8ba1\u7b97\u3001\u72b6\u6001\u6d41\u8f6c\u3001\u53d6\u6d88\u903b\u8f91\u3002
 * \u4f9d\u8d56\u5168\u90e8 Mock\uff0c\u4e0d\u542f\u52a8 Spring \u4e0a\u4e0b\u6587\u3002
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalesOrderServiceImpl \u2014 \u9500\u552e\u8ba2\u5355\u670d\u52a1")
class SalesOrderServiceImplTest {

    @Mock private SalesOrderMapper salesOrderMapper;
    @Mock private SalesOrderItemMapper salesOrderItemMapper;
    @Mock private ProductMapper productMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private OrderNoGenerator orderNoGenerator;
    @Mock private StockWarningNotificationService notificationService;

    @InjectMocks
    private SalesOrderServiceImpl salesOrderService;

    @BeforeEach
    void setUp() {
        // cancel() uses TransactionSynchronizationManager.registerSynchronization()
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ===========================================================
    // \u8f85\u52a9\u65b9\u6cd5
    // ===========================================================

    private PmsProduct buildProduct(Long id, Long enterpriseId, BigDecimal price, BigDecimal costPrice) {
        PmsProduct p = new PmsProduct();
        p.setId(id);
        p.setEnterpriseId(enterpriseId);
        p.setName("\u5546\u54c1-" + id);
        p.setUnit("\u7bb1");
        p.setPrice(price);
        p.setCostPrice(costPrice);
        p.setStock(100);
        p.setStatus(1);
        return p;
    }

    private OmsSalesOrder buildOrder(Long id, Long enterpriseId, String status) {
        OmsSalesOrder o = new OmsSalesOrder();
        o.setId(id);
        o.setEnterpriseId(enterpriseId);
        o.setStatus(status);
        o.setPaymentStatus(OrderConstants.PAY_UNPAID);
        o.setOrderSource(OrderConstants.ORDER_SOURCE_MERCHANT);
        o.setTotalAmount(BigDecimal.ZERO);
        o.setTotalCost(BigDecimal.ZERO);
        o.setTotalProfit(BigDecimal.ZERO);
        return o;
    }

    // ===========================================================
    // 1. create
    // ===========================================================

    @Test
    @DisplayName("Should create order with correct total amount using price snapshot")
    void should_createOrderWithCorrectTotal_when_priceSnapshot() {
        // Arrange
        Long enterpriseId = 1L;
        BigDecimal price = new BigDecimal("25.00");
        BigDecimal costPrice = new BigDecimal("15.00");
        PmsProduct product = buildProduct(10L, enterpriseId, price, costPrice);

        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO20260315001");
        when(productMapper.selectById(10L)).thenReturn(product);
        when(productMapper.adjustStock(eq(10L), eq(-4))).thenReturn(1);

        SalesOrderRequest.OrderItemRequest item = new SalesOrderRequest.OrderItemRequest();
        item.setProductId(10L);
        item.setQuantity(4);

        SalesOrderRequest request = new SalesOrderRequest();
        request.setItems(List.of(item));

        // Act
        salesOrderService.create(enterpriseId, request);

        // Assert
        ArgumentCaptor<OmsSalesOrder> orderCaptor = ArgumentCaptor.forClass(OmsSalesOrder.class);
        verify(salesOrderMapper).insert(orderCaptor.capture());
        OmsSalesOrder saved = orderCaptor.getValue();

        assertThat(saved.getTotalAmount())
                .as("\u8ba2\u5355\u603b\u91d1\u989d\u5fc5\u987b\u7b49\u4e8e price \u00d7 quantity \u7684\u5feb\u7167\u8ba1\u7b97\u7ed3\u679c")
                .isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(saved.getTotalCost())
                .as("\u8ba2\u5355\u603b\u6210\u672c\u5fc5\u987b\u7b49\u4e8e costPrice \u00d7 quantity")
                .isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(saved.getTotalProfit())
                .as("\u8ba2\u5355\u6bdb\u5229\u6da6 = (price - costPrice) \u00d7 quantity = 10 \u00d7 4 = 40")
                .isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("Should save price snapshot in order item")
    void should_saveOriginalPrice_in_orderItem_snapshot() {
        Long enterpriseId = 1L;
        PmsProduct product = buildProduct(20L, enterpriseId, new BigDecimal("50.00"), new BigDecimal("30.00"));

        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO20260315002");
        when(productMapper.selectById(20L)).thenReturn(product);
        when(productMapper.adjustStock(eq(20L), eq(-3))).thenReturn(1);

        SalesOrderRequest.OrderItemRequest itemReq = new SalesOrderRequest.OrderItemRequest();
        itemReq.setProductId(20L);
        itemReq.setQuantity(3);

        SalesOrderRequest request = new SalesOrderRequest();
        request.setItems(List.of(itemReq));

        salesOrderService.create(enterpriseId, request);

        ArgumentCaptor<OmsSalesOrderItem> itemCaptor = ArgumentCaptor.forClass(OmsSalesOrderItem.class);
        verify(salesOrderItemMapper).insert(itemCaptor.capture());
        OmsSalesOrderItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getPrice())
                .as("\u660e\u7ec6\u5355\u4ef7\u5fc5\u987b\u5feb\u7167\u4e0b\u5355\u65f6\u7684\u5546\u54c1\u4ef7\u683c")
                .isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(savedItem.getAmount())
                .as("\u660e\u7ec6\u91d1\u989d = price \u00d7 quantity = 150.00")
                .isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when product does not belong to current enterprise")
    void should_throwNotFound_when_productBelongsToOtherEnterprise() {
        Long myEnterpriseId = 1L;
        PmsProduct product = buildProduct(30L, 999L, BigDecimal.TEN, BigDecimal.ONE);

        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO20260315003");
        when(productMapper.selectById(30L)).thenReturn(product);

        SalesOrderRequest.OrderItemRequest item = new SalesOrderRequest.OrderItemRequest();
        item.setProductId(30L);
        item.setQuantity(1);

        SalesOrderRequest request = new SalesOrderRequest();
        request.setItems(List.of(item));

        assertThatThrownBy(() -> salesOrderService.create(myEnterpriseId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Should set order status to PENDING and payment to UNPAID on creation")
    void should_setInitialStatus_when_orderCreated() {
        Long enterpriseId = 1L;
        PmsProduct product = buildProduct(40L, enterpriseId, BigDecimal.TEN, BigDecimal.ONE);

        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO20260315004");
        when(productMapper.selectById(40L)).thenReturn(product);
        when(productMapper.adjustStock(eq(40L), eq(-1))).thenReturn(1);

        SalesOrderRequest.OrderItemRequest item = new SalesOrderRequest.OrderItemRequest();
        item.setProductId(40L);
        item.setQuantity(1);

        SalesOrderRequest request = new SalesOrderRequest();
        request.setItems(List.of(item));

        salesOrderService.create(enterpriseId, request);

        verify(salesOrderMapper).insert(argThat(o ->
                OrderConstants.SALES_PENDING.equals(o.getStatus())
                        && OrderConstants.PAY_UNPAID.equals(o.getPaymentStatus())));
    }

    // ===========================================================
    // 2. confirm
    // ===========================================================

    @Test
    @DisplayName("Should change status to CONFIRMED when order is PENDING")
    void should_confirmOrder_when_statusIsPending() {
        Long enterpriseId = 1L;
        OmsSalesOrder order = buildOrder(1L, enterpriseId, OrderConstants.SALES_PENDING);
        when(salesOrderMapper.selectById(1L)).thenReturn(order);

        salesOrderService.confirm(enterpriseId, 1L);

        verify(salesOrderMapper).updateById(argThat(o ->
                OrderConstants.SALES_CONFIRMED.equals(o.getStatus())));
    }

    @Test
    @DisplayName("Should throw ORDER_STATUS_ERROR when confirming a CANCELLED order")
    void should_throwOrderStatusError_when_confirmingCancelledOrder() {
        OmsSalesOrder order = buildOrder(2L, 1L, OrderConstants.SALES_CANCELLED);
        when(salesOrderMapper.selectById(2L)).thenReturn(order);

        assertThatThrownBy(() -> salesOrderService.confirm(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
    }

    // ===========================================================
    // 3. ship
    // ===========================================================

    @Test
    @DisplayName("Should change status to SHIPPED when order is CONFIRMED")
    void should_shipOrder_when_statusIsConfirmed() {
        OmsSalesOrder order = buildOrder(3L, 1L, OrderConstants.SALES_CONFIRMED);
        order.setPaymentStatus(OrderConstants.PAY_PAID);
        when(salesOrderMapper.selectById(3L)).thenReturn(order);

        salesOrderService.ship(1L, 3L);

        verify(salesOrderMapper).updateById(argThat(o ->
                OrderConstants.SALES_SHIPPED.equals(o.getStatus())));
    }

    @Test
    @DisplayName("Should throw ORDER_STATUS_ERROR when shipping a PENDING order")
    void should_throwOrderStatusError_when_shippingPendingOrder() {
        OmsSalesOrder order = buildOrder(4L, 1L, OrderConstants.SALES_PENDING);
        when(salesOrderMapper.selectById(4L)).thenReturn(order);

        assertThatThrownBy(() -> salesOrderService.ship(1L, 4L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
    }

    // ===========================================================
    // 4. complete
    // ===========================================================

    @Test
    @DisplayName("Should change status to COMPLETED when order is SHIPPED")
    void should_completeOrder_when_statusIsShipped() {
        OmsSalesOrder order = buildOrder(5L, 1L, OrderConstants.SALES_SHIPPED);
        order.setPaymentStatus(OrderConstants.PAY_PAID);
        when(salesOrderMapper.selectById(5L)).thenReturn(order);

        salesOrderService.complete(1L, 5L);

        verify(salesOrderMapper).updateById(argThat(o ->
                OrderConstants.SALES_COMPLETED.equals(o.getStatus())));
    }

    @Test
    @DisplayName("Should throw ORDER_STATUS_ERROR when completing a CANCELLED order")
    void should_throwOrderStatusError_when_completingCancelledOrder() {
        OmsSalesOrder order = buildOrder(6L, 1L, OrderConstants.SALES_CANCELLED);
        when(salesOrderMapper.selectById(6L)).thenReturn(order);

        assertThatThrownBy(() -> salesOrderService.complete(1L, 6L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
    }

    // ===========================================================
    // 5. cancel
    // ===========================================================

    @Test
    @DisplayName("Should change status to CANCELLED when cancelling a PENDING order")
    void should_cancelOrder_when_statusIsPending() {
        OmsSalesOrder order = buildOrder(7L, 1L, OrderConstants.SALES_PENDING);
        order.setOrderSource(OrderConstants.ORDER_SOURCE_MERCHANT);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);
        when(salesOrderMapper.selectById(7L)).thenReturn(order);

        salesOrderService.cancel(1L, 7L, 100L, null);

        verify(salesOrderMapper).updateById(argThat(o ->
                OrderConstants.SALES_CANCELLED.equals(o.getStatus())));
    }

    @Test
    @DisplayName("Should throw ORDER_STATUS_ERROR when cancelling a COMPLETED order")
    void should_throwOrderStatusError_when_cancellingCompletedOrder() {
        OmsSalesOrder order = buildOrder(8L, 1L, OrderConstants.SALES_COMPLETED);
        when(salesOrderMapper.selectById(8L)).thenReturn(order);

        assertThatThrownBy(() -> salesOrderService.cancel(1L, 8L, 100L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
    }

    @Test
    @DisplayName("Should throw ORDER_STATUS_ERROR when cancelling a SHIPPED order")
    void should_throwOrderStatusError_when_cancellingShippedOrder() {
        OmsSalesOrder order = buildOrder(9L, 1L, OrderConstants.SALES_SHIPPED);
        when(salesOrderMapper.selectById(9L)).thenReturn(order);

        assertThatThrownBy(() -> salesOrderService.cancel(1L, 9L, 100L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
    }

    // ===========================================================
    // 6. getById
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when accessing another enterprise's order")
    void should_throwNotFound_when_accessingOtherEnterpriseOrder() {
        OmsSalesOrder order = buildOrder(10L, 999L, OrderConstants.SALES_PENDING);
        when(salesOrderMapper.selectById(10L)).thenReturn(order);

        assertThatThrownBy(() -> salesOrderService.getById(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Should return order when enterprise id matches")
    void should_returnOrder_when_enterpriseIdMatches() {
        OmsSalesOrder order = buildOrder(11L, 1L, OrderConstants.SALES_PENDING);
        when(salesOrderMapper.selectById(11L)).thenReturn(order);
        when(salesOrderItemMapper.selectList(any())).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> salesOrderService.getById(1L, 11L));
    }

    // ===========================================================
    // 7. BUG-1: stock deduction on create
    // ===========================================================

    @Test
    @DisplayName("Should deduct stock immediately on merchant order creation (BUG-1 fix)")
    void should_deductStock_when_merchantOrderCreated() {
        Long enterpriseId = 1L;
        PmsProduct product = buildProduct(50L, enterpriseId, BigDecimal.TEN, BigDecimal.ONE);
        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO20260327001");
        when(productMapper.selectById(50L)).thenReturn(product);
        when(productMapper.adjustStock(50L, -3)).thenReturn(1);

        SalesOrderRequest.OrderItemRequest item = new SalesOrderRequest.OrderItemRequest();
        item.setProductId(50L);
        item.setQuantity(3);

        SalesOrderRequest request = new SalesOrderRequest();
        request.setItems(List.of(item));

        salesOrderService.create(enterpriseId, request);

        verify(productMapper).adjustStock(50L, -3);
    }

    @Test
    @DisplayName("Should throw STOCK_INSUFFICIENT when stock is not enough on create (BUG-1)")
    void should_throwStockInsufficient_when_stockNotEnoughOnCreate() {
        Long enterpriseId = 1L;
        PmsProduct product = buildProduct(60L, enterpriseId, BigDecimal.TEN, BigDecimal.ONE);
        product.setStock(0);
        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO20260327002");
        when(productMapper.selectById(60L)).thenReturn(product);
        when(productMapper.adjustStock(60L, -5)).thenReturn(0);

        SalesOrderRequest.OrderItemRequest item = new SalesOrderRequest.OrderItemRequest();
        item.setProductId(60L);
        item.setQuantity(5);

        SalesOrderRequest request = new SalesOrderRequest();
        request.setItems(List.of(item));

        assertThatThrownBy(() -> salesOrderService.create(enterpriseId, request))
                .isInstanceOf(BusinessException.class);
    }

    // ===========================================================
    // 8. BUG-1: cancel() stock restore
    // ===========================================================

    @Test
    @DisplayName("Should restore stock on cancel even when order is UNPAID (BUG-1 fix)")
    void should_restoreStock_when_cancellingUnpaidMerchantOrder() {
        OmsSalesOrder order = buildOrder(12L, 1L, OrderConstants.SALES_PENDING);
        order.setOrderSource(OrderConstants.ORDER_SOURCE_MERCHANT);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);
        when(salesOrderMapper.selectById(12L)).thenReturn(order);

        OmsSalesOrderItem item = new OmsSalesOrderItem();
        item.setProductId(50L);
        item.setQuantity(3);
        when(salesOrderItemMapper.selectList(any())).thenReturn(List.of(item));

        salesOrderService.cancel(1L, 12L, 100L, null);

        verify(productMapper).adjustStock(50L, 3);
    }

    // ===========================================================
    // 9. BUG-1: pay() no longer deducts stock
    // ===========================================================

    @Test
    @DisplayName("Should NOT deduct stock on pay (BUG-1 fix)")
    void should_notDeductStock_when_payingOrder() {
        OmsSalesOrder order = buildOrder(13L, 1L, OrderConstants.SALES_PENDING);
        order.setOrderSource(OrderConstants.ORDER_SOURCE_MERCHANT);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);
        when(salesOrderMapper.selectById(13L)).thenReturn(order);

        salesOrderService.confirmPayment(1L, 13L, 100L, null);

        verify(productMapper, never()).adjustStock(any(), anyInt());
        verify(salesOrderMapper).updateById(argThat(o ->
                OrderConstants.PAY_PAID.equals(o.getPaymentStatus())));
    }

    @Test
    @DisplayName("Should throw ORDER_STATUS_ERROR when shipping an unpaid order in strict mode")
    void should_throwOrderStatusError_when_shippingUnpaidOrder() {
        OmsSalesOrder order = buildOrder(14L, 1L, OrderConstants.SALES_CONFIRMED);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);
        when(salesOrderMapper.selectById(14L)).thenReturn(order);

        assertThatThrownBy(() -> salesOrderService.ship(1L, 14L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
    }

    @Test
    @DisplayName("Should throw ORDER_STATUS_ERROR when completing an unpaid order in strict mode")
    void should_throwOrderStatusError_when_completingUnpaidOrder() {
        OmsSalesOrder order = buildOrder(15L, 1L, OrderConstants.SALES_SHIPPED);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);
        when(salesOrderMapper.selectById(15L)).thenReturn(order);

        assertThatThrownBy(() -> salesOrderService.complete(1L, 15L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
    }

    @Test
    @DisplayName("Should allow sales role to confirm payment")
    void should_allowSalesRole_when_confirmingPayment() {
        OmsSalesOrder order = buildOrder(16L, 1L, OrderConstants.SALES_PENDING);
        order.setPaymentStatus(OrderConstants.PAY_CLAIMED);
        when(salesOrderMapper.selectById(16L)).thenReturn(order);

        salesOrderService.confirmPayment(1L, 16L, 36L, "SALES");

        verify(salesOrderMapper).updateById(argThat(o ->
                OrderConstants.PAY_PAID.equals(o.getPaymentStatus())
                        && o.getRemark() != null
                        && o.getRemark().contains("CONFIRM_PAYMENT")
                        && o.getRemark().contains("SALES:36")));
    }

    @Test
    @DisplayName("Should allow sales role to cancel a confirmed paid order with audit remark")
    void should_allowSalesRole_when_cancellingConfirmedPaidOrder() {
        OmsSalesOrder order = buildOrder(17L, 1L, OrderConstants.SALES_CONFIRMED);
        order.setPaymentStatus(OrderConstants.PAY_PAID);
        when(salesOrderMapper.selectById(17L)).thenReturn(order);

        OmsSalesOrderItem item = new OmsSalesOrderItem();
        item.setProductId(70L);
        item.setQuantity(2);
        when(salesOrderItemMapper.selectList(any())).thenReturn(List.of(item));

        salesOrderService.cancel(1L, 17L, 36L, "SALES");

        verify(salesOrderMapper).updateById(argThat(o ->
                OrderConstants.SALES_CANCELLED.equals(o.getStatus())
                        && o.getRemark() != null
                        && o.getRemark().contains("\u5df2\u63d0\u9192\u7ebf\u4e0b\u9000\u6b3e")
                        && o.getRemark().contains("SALES:36")));
    }
}
