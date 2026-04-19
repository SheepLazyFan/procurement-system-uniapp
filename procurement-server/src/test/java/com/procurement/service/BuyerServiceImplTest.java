package com.procurement.service;

import com.procurement.common.constant.OrderConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.BuyerOrderRequest;
import com.procurement.entity.CrmCustomer;
import com.procurement.entity.OmsSalesOrder;
import com.procurement.entity.PmsProduct;
import com.procurement.entity.SysEnterprise;
import com.procurement.entity.SysUser;
import com.procurement.mapper.*;
import com.procurement.service.impl.BuyerServiceImpl;
import com.procurement.common.util.OrderNoGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BuyerServiceImpl 单元测试
 * <p>
 * 测试范围：买家下单流程、库存原子扣减、企业数据隔离、下架商品拒绝。
 * 依赖全部 Mock，不启动 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BuyerServiceImpl — 买家端服务")
class BuyerServiceImplTest {

    @Mock private EnterpriseMapper enterpriseMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ProductMapper productMapper;
    @Mock private SalesOrderMapper salesOrderMapper;
    @Mock private SalesOrderItemMapper salesOrderItemMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private UserMapper userMapper;
    @Mock private OrderNoGenerator orderNoGenerator;
    @Mock private StockWarningNotificationService notificationService;

    @InjectMocks
    private BuyerServiceImpl buyerService;

    // ===========================================================
    // 辅助方法
    // ===========================================================

    private SysEnterprise buildEnterprise(Long id) {
        SysEnterprise e = new SysEnterprise();
        e.setId(id);
        e.setName("测试企业-" + id);
        return e;
    }

    private PmsProduct buildProduct(Long id, Long enterpriseId, int stock, int status) {
        PmsProduct p = new PmsProduct();
        p.setId(id);
        p.setEnterpriseId(enterpriseId);
        p.setName("买家商品-" + id);
        p.setUnit("个");
        p.setPrice(new BigDecimal("20.00"));
        p.setCostPrice(new BigDecimal("12.00"));
        p.setStock(stock);
        p.setStatus(status);
        return p;
    }

    private SysUser buildBuyer(Long id, String openid) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setWxOpenid(openid);
        u.setNickName("买家-" + id);
        return u;
    }

    private BuyerOrderRequest buildRequest(Long enterpriseId, Long productId, int qty) {
        BuyerOrderRequest.BuyerItemRequest item = new BuyerOrderRequest.BuyerItemRequest();
        item.setProductId(productId);
        item.setQuantity(qty);

        BuyerOrderRequest req = new BuyerOrderRequest();
        req.setEnterpriseId(enterpriseId);
        req.setItems(List.of(item));
        return req;
    }

    // ===========================================================
    // 1. createOrder — 正常下单
    // ===========================================================

    @Test
    @DisplayName("Should create order with source BUYER when buyer places order")
    void should_createOrderWithSourceBuyer_when_buyerPlacesOrder() {
        // Arrange
        Long enterpriseId = 1L;
        Long buyerId = 100L;
        Long productId = 10L;

        when(enterpriseMapper.selectById(enterpriseId)).thenReturn(buildEnterprise(enterpriseId));
        when(userMapper.selectById(buyerId)).thenReturn(buildBuyer(buyerId, "openid_100"));
        when(customerMapper.selectOne(any())).thenReturn(null); // 新买家，自动创建客户
        when(productMapper.selectById(productId)).thenReturn(buildProduct(productId, enterpriseId, 50, 1));
        when(productMapper.adjustStock(eq(productId), eq(-2))).thenReturn(1);
        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO20260315001");

        BuyerOrderRequest request = buildRequest(enterpriseId, productId, 2);

        // Act
        buyerService.createOrder(buyerId, request);

        // Assert — 订单来源必须是 BUYER
        verify(salesOrderMapper).insert(argThat(o ->
                OrderConstants.ORDER_SOURCE_BUYER.equals(o.getOrderSource())));
    }

    @Test
    @DisplayName("Should atomically deduct stock when buyer creates order")
    void should_deductStock_when_buyerCreatesOrder() {
        // Arrange
        Long enterpriseId = 1L;
        Long buyerId = 101L;
        Long productId = 11L;

        when(enterpriseMapper.selectById(enterpriseId)).thenReturn(buildEnterprise(enterpriseId));
        when(userMapper.selectById(buyerId)).thenReturn(buildBuyer(buyerId, "openid_101"));
        when(customerMapper.selectOne(any())).thenReturn(null);
        when(productMapper.selectById(productId)).thenReturn(buildProduct(productId, enterpriseId, 30, 1));
        when(productMapper.adjustStock(eq(productId), eq(-5))).thenReturn(1);
        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO20260315002");

        BuyerOrderRequest request = buildRequest(enterpriseId, productId, 5);

        // Act
        buyerService.createOrder(buyerId, request);

        // Assert — productMapper.adjustStock 以 -5 调用（原子扣减）
        verify(productMapper).adjustStock(productId, -5);
    }

    // ===========================================================
    // 2. createOrder — 库存不足
    // ===========================================================

    @Test
    @DisplayName("Should throw STOCK_INSUFFICIENT when product stock is insufficient")
    void should_throwStockInsufficient_when_stockIsInsufficient() {
        // Arrange
        Long enterpriseId = 1L;
        Long buyerId = 102L;
        Long productId = 12L;

        when(enterpriseMapper.selectById(enterpriseId)).thenReturn(buildEnterprise(enterpriseId));
        when(userMapper.selectById(buyerId)).thenReturn(buildBuyer(buyerId, "openid_102"));
        when(customerMapper.selectOne(any())).thenReturn(null);
        when(productMapper.selectById(productId)).thenReturn(buildProduct(productId, enterpriseId, 3, 1));
        when(productMapper.adjustStock(eq(productId), eq(-10))).thenReturn(0); // 库存不足，DB 返回 0

        BuyerOrderRequest request = buildRequest(enterpriseId, productId, 10);

        // Act & Assert
        assertThatThrownBy(() -> buyerService.createOrder(buyerId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .as("库存不足时必须返回 STOCK_INSUFFICIENT(40902)")
                        .isEqualTo(ResultCode.STOCK_INSUFFICIENT.getCode()));
    }

    // ===========================================================
    // 3. createOrder — 商品已下架拒绝
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when product is off-shelf (status=0)")
    void should_throwNotFound_when_productIsOffShelf() {
        // Arrange
        Long enterpriseId = 1L;
        Long buyerId = 103L;
        Long productId = 13L;

        when(enterpriseMapper.selectById(enterpriseId)).thenReturn(buildEnterprise(enterpriseId));
        when(userMapper.selectById(buyerId)).thenReturn(buildBuyer(buyerId, "openid_103"));
        when(customerMapper.selectOne(any())).thenReturn(null);
        // 商品 status=0（已下架）
        when(productMapper.selectById(productId)).thenReturn(buildProduct(productId, enterpriseId, 100, 0));
        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO_ANY");

        BuyerOrderRequest request = buildRequest(enterpriseId, productId, 1);

        // Act & Assert
        assertThatThrownBy(() -> buyerService.createOrder(buyerId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    // ===========================================================
    // 4. createOrder — 企业数据隔离
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when product belongs to different enterprise")
    void should_throwNotFound_when_productBelongsToDifferentEnterprise() {
        // Arrange
        Long buyerEnterpriseId = 1L;   // 买家请求的企业
        Long productEnterpriseId = 999L; // 商品实际所属企业（不同）
        Long buyerId = 104L;
        Long productId = 14L;

        when(enterpriseMapper.selectById(buyerEnterpriseId)).thenReturn(buildEnterprise(buyerEnterpriseId));
        when(userMapper.selectById(buyerId)).thenReturn(buildBuyer(buyerId, "openid_104"));
        when(customerMapper.selectOne(any())).thenReturn(null);
        // 商品属于企业 999，不是 1
        when(productMapper.selectById(productId)).thenReturn(buildProduct(productId, productEnterpriseId, 100, 1));
        when(orderNoGenerator.generate(any(), any(), any())).thenReturn("SO_ANY");

        BuyerOrderRequest request = buildRequest(buyerEnterpriseId, productId, 1);

        // Act & Assert — 跨企业下单被拒绝
        assertThatThrownBy(() -> buyerService.createOrder(buyerId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    // ===========================================================
    // 5. createOrder — 企业不存在
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when enterprise does not exist")
    void should_throwNotFound_when_enterpriseDoesNotExist() {
        // Arrange
        Long nonExistentEnterpriseId = 9999L;
        when(enterpriseMapper.selectById(nonExistentEnterpriseId)).thenReturn(null);

        BuyerOrderRequest request = buildRequest(nonExistentEnterpriseId, 1L, 1);

        // Act & Assert
        assertThatThrownBy(() -> buyerService.createOrder(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    // ===========================================================
    // 6. getStoreInfo — 公开接口
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when store enterprise does not exist")
    void should_throwNotFound_when_storeEnterpriseDoesNotExist() {
        // Arrange
        when(enterpriseMapper.selectById(8888L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> buyerService.getStoreInfo(8888L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Should return store info when enterprise exists")
    void should_returnStoreInfo_when_enterpriseExists() {
        // Arrange
        SysEnterprise enterprise = buildEnterprise(1L);
        enterprise.setLogoUrl("https://logo.example.com/img.png");
        enterprise.setContactPhone("13800138000");

        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise);
        when(categoryMapper.selectCount(any())).thenReturn(3L);
        when(productMapper.selectCount(any())).thenReturn(15L);

        // Act
        var result = buyerService.getStoreInfo(1L);

        // Assert
        assertThat(result.get("enterpriseName")).isEqualTo("测试企业-1");
        assertThat(result.get("categoryCount")).isEqualTo(3L);
        assertThat(result.get("productCount")).isEqualTo(15L);
    }

    @Test
    @DisplayName("Should downgrade legacy payOrder to CLAIMED instead of PAID")
    void should_downgradeLegacyPayOrder_toClaimed() {
        // Arrange
        Long buyerId = 201L;
        OmsSalesOrder order = new OmsSalesOrder();
        order.setId(301L);
        order.setEnterpriseId(1L);
        order.setOrderNo("SO301");
        order.setCustomerId(401L);
        order.setStatus(OrderConstants.SALES_PENDING);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);

        CrmCustomer customer = new CrmCustomer();
        customer.setId(401L);
        customer.setWxOpenid("openid_201");

        when(userMapper.selectById(buyerId)).thenReturn(buildBuyer(buyerId, "openid_201"));
        when(salesOrderMapper.selectById(301L)).thenReturn(order);
        when(customerMapper.selectById(401L)).thenReturn(customer);

        // Act
        buyerService.payOrder(buyerId, 301L);

        // Assert
        verify(salesOrderMapper).updateById(argThat(o ->
                OrderConstants.PAY_CLAIMED.equals(o.getPaymentStatus())
                        && o.getRemark() != null
                        && o.getRemark().contains("LEGACY_PAY_COMPAT")
                        && o.getRemark().contains("BUYER:201")));
    }

    @Test
    @DisplayName("Should throw ORDER_STATUS_ERROR when buyer claims paid on cancelled order")
    void should_throwOrderStatusError_when_claimPaidOnCancelledOrder() {
        // Arrange
        Long buyerId = 202L;
        OmsSalesOrder order = new OmsSalesOrder();
        order.setId(302L);
        order.setEnterpriseId(1L);
        order.setCustomerId(402L);
        order.setStatus(OrderConstants.SALES_CANCELLED);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);

        CrmCustomer customer = new CrmCustomer();
        customer.setId(402L);
        customer.setWxOpenid("openid_202");

        when(userMapper.selectById(buyerId)).thenReturn(buildBuyer(buyerId, "openid_202"));
        when(salesOrderMapper.selectById(302L)).thenReturn(order);
        when(customerMapper.selectById(402L)).thenReturn(customer);

        // Act & Assert
        assertThatThrownBy(() -> buyerService.claimPaid(buyerId, 302L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
    }

    @Test
    @DisplayName("Should throw ORDER_STATUS_ERROR when buyer cancels claimed order")
    void should_throwOrderStatusError_when_cancellingClaimedOrder() {
        // Arrange
        Long buyerId = 203L;
        OmsSalesOrder order = new OmsSalesOrder();
        order.setId(303L);
        order.setEnterpriseId(1L);
        order.setCustomerId(403L);
        order.setStatus(OrderConstants.SALES_PENDING);
        order.setPaymentStatus(OrderConstants.PAY_CLAIMED);

        CrmCustomer customer = new CrmCustomer();
        customer.setId(403L);
        customer.setWxOpenid("openid_203");

        when(userMapper.selectById(buyerId)).thenReturn(buildBuyer(buyerId, "openid_203"));
        when(salesOrderMapper.selectById(303L)).thenReturn(order);
        when(customerMapper.selectById(403L)).thenReturn(customer);

        // Act & Assert
        assertThatThrownBy(() -> buyerService.cancelOrder(buyerId, 303L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode()));
    }

    // ===========================================================
    // 7. getProductDetail — 低库存阈值策略 (T-01 ~ T-07)
    //    验证 BUYER_LOW_STOCK_THRESHOLD = 20 的 3-tier 库存显示
    // ===========================================================

    /**
     * 测试数据源：
     * T-01: stock=null  → OUT_OF_STOCK, 返回 stock=0（防御性兜底）
     * T-02: stock=0     → OUT_OF_STOCK, 返回 stock=0（精确边界）
     * T-03: stock=-5    → OUT_OF_STOCK, 返回 stock=0（超卖异常兜底）
     * T-04: stock=1     → LOW_STOCK,    返回 stock=1（低库存下边界）
     * T-05: stock=20    → LOW_STOCK,    返回 stock=20（阈值边界，含等于）
     * T-06: stock=21    → IN_STOCK,     不返回 stock 字段（超过阈值）
     * T-07: stock=100   → IN_STOCK,     不返回 stock 字段（正常库存）
     */
    static Stream<Arguments> lowStockThresholdProvider() {
        return Stream.of(
                // inputStock, expectedStockStatus, expectedStockValue (null = key absent)
                Arguments.of(null, "OUT_OF_STOCK", 0),     // T-01
                Arguments.of(0,    "OUT_OF_STOCK", 0),     // T-02
                Arguments.of(-5,   "OUT_OF_STOCK", 0),     // T-03
                Arguments.of(1,    "LOW_STOCK",    1),     // T-04
                Arguments.of(20,   "LOW_STOCK",    20),    // T-05
                Arguments.of(21,   "IN_STOCK",     null),  // T-06
                Arguments.of(100,  "IN_STOCK",     null)   // T-07
        );
    }

    @ParameterizedTest(name = "T-0{index}: stock={0} → status={1}, stockValue={2}")
    @MethodSource("lowStockThresholdProvider")
    @DisplayName("getProductDetail — 低库存阈值策略")
    void should_returnCorrectStockStatus_when_stockVaries(
            Integer inputStock, String expectedStatus, Integer expectedStockValue) {
        // Arrange
        Long productId = 50L;
        Long enterpriseId = 1L;

        PmsProduct product = new PmsProduct();
        product.setId(productId);
        product.setEnterpriseId(enterpriseId);
        product.setName("阈值测试商品");
        product.setUnit("个");
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(inputStock); // Integer 类型，支持 null
        product.setStatus(1);

        when(productMapper.selectById(productId)).thenReturn(product);
        when(enterpriseMapper.selectById(enterpriseId)).thenReturn(buildEnterprise(enterpriseId));

        // Act
        Map<String, Object> result = buyerService.getProductDetail(productId);

        // Assert — stockStatus
        assertThat(result.get("stockStatus"))
                .as("stock=%s 时 stockStatus 应为 %s", inputStock, expectedStatus)
                .isEqualTo(expectedStatus);

        // Assert — stock 值
        if (expectedStockValue != null) {
            assertThat(result.get("stock"))
                    .as("stock=%s 时应返回精确库存值 %d", inputStock, expectedStockValue)
                    .isEqualTo(expectedStockValue);
        } else {
            assertThat(result.containsKey("stock"))
                    .as("stock=%s (IN_STOCK) 时不应暴露精确库存字段", inputStock)
                    .isFalse();
        }
    }
}

