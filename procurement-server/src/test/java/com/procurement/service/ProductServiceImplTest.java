package com.procurement.service;

import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.ProductRequest;
import com.procurement.dto.request.StockAdjustRequest;
import com.procurement.entity.PmsProduct;
import com.procurement.mapper.CategoryMapper;
import com.procurement.mapper.ProductMapper;
import com.procurement.mapper.ProductSupplierMapper;
import com.procurement.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ProductServiceImpl 单元测试
 * <p>
 * 测试范围：库存调整逻辑、企业数据隔离、商品创建/删除校验。
 * 依赖全部 Mock，不启动 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl — 商品管理服务")
class ProductServiceImplTest {

    @Mock private ProductMapper productMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ProductSupplierMapper productSupplierMapper;
    @Mock private StockWarningNotificationService notificationService;

    @InjectMocks
    private ProductServiceImpl productService;

    // ===========================================================
    // 辅助方法 — 构建测试商品
    // ===========================================================

    private PmsProduct buildProduct(Long id, Long enterpriseId, int stock) {
        PmsProduct p = new PmsProduct();
        p.setId(id);
        p.setEnterpriseId(enterpriseId);
        p.setName("测试商品");
        p.setUnit("箱");
        p.setPrice(new BigDecimal("10.00"));
        p.setCostPrice(new BigDecimal("6.00"));
        p.setStock(stock);
        p.setStockWarning(5);
        p.setStatus(1);
        return p;
    }

    // ===========================================================
    // 1. adjustStock — 库存调整
    // ===========================================================

    @Test
    @DisplayName("Should increase stock when adjustment type is IN")
    void should_increaseStock_when_typeIsIN() {
        // Arrange
        Long enterpriseId = 1L;
        Long productId = 10L;
        PmsProduct product = buildProduct(productId, enterpriseId, 50);

        StockAdjustRequest request = new StockAdjustRequest();
        request.setProductId(productId);
        request.setQuantity(20);
        request.setType("IN");

        when(productMapper.selectById(productId)).thenReturn(product);
        when(productMapper.adjustStock(productId, 20)).thenReturn(1); // 1 row affected

        // Act
        Integer newStock = productService.adjustStock(enterpriseId, productId, request, "SELLER");

        // Assert — adjustStock 以 +20 调用
        verify(productMapper).adjustStock(productId, 20);
    }

    @Test
    @DisplayName("Should decrease stock when adjustment type is OUT and stock is sufficient")
    void should_decreaseStock_when_typeIsOUT_andStockSufficient() {
        // Arrange
        Long enterpriseId = 1L;
        Long productId = 11L;
        PmsProduct product = buildProduct(productId, enterpriseId, 30);

        StockAdjustRequest request = new StockAdjustRequest();
        request.setProductId(productId);
        request.setQuantity(10);
        request.setType("OUT");

        when(productMapper.selectById(productId)).thenReturn(product);
        when(productMapper.adjustStock(productId, -10)).thenReturn(1);

        // Act
        productService.adjustStock(enterpriseId, productId, request, "SELLER");

        // Assert — 以负数 -10 调用（出库）
        verify(productMapper).adjustStock(productId, -10);
    }

    @Test
    @DisplayName("Should throw STOCK_INSUFFICIENT when OUT quantity exceeds current stock")
    void should_throwStockInsufficient_when_outQuantityExceedsStock() {
        // Arrange
        Long enterpriseId = 1L;
        Long productId = 12L;
        PmsProduct product = buildProduct(productId, enterpriseId, 5); // 只有 5 件

        StockAdjustRequest request = new StockAdjustRequest();
        request.setProductId(productId);
        request.setQuantity(10); // 要出 10 件
        request.setType("OUT");

        when(productMapper.selectById(productId)).thenReturn(product);
        when(productMapper.adjustStock(productId, -10)).thenReturn(0); // 0 = 库存不足，DB 拒绝

        // Act & Assert
        assertThatThrownBy(() -> productService.adjustStock(enterpriseId, productId, request, "SELLER"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .as("库存不足时必须抛出 STOCK_INSUFFICIENT(40902)")
                        .isEqualTo(ResultCode.STOCK_INSUFFICIENT.getCode()));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when adjusting stock of another enterprise's product")
    void should_throwNotFound_when_adjustStockOfOtherEnterpriseProduct() {
        // Arrange
        Long myEnterpriseId = 1L;
        Long otherEnterpriseId = 999L;
        Long productId = 20L;
        PmsProduct product = buildProduct(productId, otherEnterpriseId, 100); // 属于其他企业

        StockAdjustRequest request = new StockAdjustRequest();
        request.setProductId(productId);
        request.setQuantity(5);
        request.setType("IN");

        when(productMapper.selectById(productId)).thenReturn(product);

        // Act & Assert — 企业数据隔离
        assertThatThrownBy(() -> productService.adjustStock(myEnterpriseId, productId, request, "SELLER"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when adjusting stock of non-existent product")
    void should_throwNotFound_when_productDoesNotExist() {
        // Arrange
        when(productMapper.selectById(999L)).thenReturn(null);

        StockAdjustRequest request = new StockAdjustRequest();
        request.setProductId(999L);
        request.setQuantity(5);
        request.setType("IN");

        // Act & Assert
        assertThatThrownBy(() -> productService.adjustStock(1L, 999L, request, "SELLER"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Should call notificationService.clearDedupOnRestock after IN adjustment")
    void should_callClearDedup_when_stockIsIncreasedByIN() {
        // Arrange
        Long enterpriseId = 1L;
        Long productId = 30L;
        PmsProduct product = buildProduct(productId, enterpriseId, 3);

        StockAdjustRequest request = new StockAdjustRequest();
        request.setProductId(productId);
        request.setQuantity(10);
        request.setType("IN");

        when(productMapper.selectById(productId)).thenReturn(product);
        when(productMapper.adjustStock(productId, 10)).thenReturn(1);

        // Act
        productService.adjustStock(enterpriseId, productId, request, "SELLER");

        // Assert — 入库后必须清除去重 key，确保下次跌破阈值时仍能发通知
        verify(notificationService).clearDedupOnRestock(
                argThat(ids -> ids.contains(productId)), eq(enterpriseId));
    }

    // ===========================================================
    // 2. getById — 企业数据隔离
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when accessing another enterprise's product detail")
    void should_throwNotFound_when_accessingOtherEnterpriseProductDetail() {
        // Arrange
        Long myEnterpriseId = 1L;
        PmsProduct product = buildProduct(50L, 999L, 10); // 属于企业 999

        when(productMapper.selectById(50L)).thenReturn(product);

        // Act & Assert
        assertThatThrownBy(() -> productService.getById(myEnterpriseId, 50L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    // ===========================================================
    // 3. create — 商品创建
    // ===========================================================

    @Test
    @DisplayName("Should create product with default stock 0 when stock is not provided")
    void should_createProductWithZeroStock_when_stockNotProvided() {
        // Arrange
        Long enterpriseId = 1L;
        ProductRequest request = new ProductRequest();
        request.setCategoryId(1L);
        request.setName("新商品");
        request.setUnit("个");
        request.setPrice(new BigDecimal("29.99"));
        request.setStock(null); // 未提供库存

        // Act
        productService.create(enterpriseId, request);

        // Assert — 新建商品库存默认为 0
        verify(productMapper).insert(argThat(p -> p.getStock() == 0));
    }

    @Test
    @DisplayName("Should create product with status 1 (on-shelf) by default")
    void should_createProductWithStatusOnShelf_byDefault() {
        // Arrange
        Long enterpriseId = 1L;
        ProductRequest request = new ProductRequest();
        request.setCategoryId(1L);
        request.setName("商品A");
        request.setUnit("箱");
        request.setPrice(new BigDecimal("99.00"));

        // Act
        productService.create(enterpriseId, request);

        // Assert — 默认上架
        verify(productMapper).insert(argThat(p -> p.getStatus() == 1));
    }

    // ===========================================================
    // 4. delete — 删除商品隔离
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when deleting another enterprise's product")
    void should_throwNotFound_when_deletingOtherEnterpriseProduct() {
        // Arrange
        PmsProduct product = buildProduct(100L, 888L, 0); // 企业 888 的商品
        when(productMapper.selectById(100L)).thenReturn(product);

        // Act & Assert
        assertThatThrownBy(() -> productService.delete(1L, 100L)) // 企业 1 试图删除
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    // ===========================================================
    // 5. 参数化：OUT 调整 quantity 边界
    // ===========================================================

    static Stream<Arguments> stockCases() {
        return Stream.of(
                Arguments.of(10, 10, false),   // 恰好用完，允许
                Arguments.of(10, 9,  false),   // 有余量，允许
                Arguments.of(10, 11, true),    // 超出，拒绝
                Arguments.of(0,  1,  true),    // 零库存出库，拒绝
                Arguments.of(1,  1,  false)    // 最后1件，允许
        );
    }

    @ParameterizedTest(name = "stock={0}, outQty={1}, expectFail={2}")
    @MethodSource("stockCases")
    @DisplayName("Should handle stock OUT boundary correctly")
    void should_handleStockOutBoundary(int currentStock, int outQty, boolean expectFail) {
        // Arrange
        Long productId = 200L;
        Long enterpriseId = 1L;
        PmsProduct product = buildProduct(productId, enterpriseId, currentStock);

        StockAdjustRequest request = new StockAdjustRequest();
        request.setProductId(productId);
        request.setQuantity(outQty);
        request.setType("OUT");

        when(productMapper.selectById(productId)).thenReturn(product);
        // adjustStock 返回 0 表示库存不足（SQL WHERE 条件不满足）
        int affected = expectFail ? 0 : 1;
        when(productMapper.adjustStock(productId, -outQty)).thenReturn(affected);

        // Act & Assert
        if (expectFail) {
            assertThatThrownBy(() -> productService.adjustStock(enterpriseId, productId, request, "SELLER"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.STOCK_INSUFFICIENT.getCode()));
        } else {
            assertThatNoException().isThrownBy(
                    () -> productService.adjustStock(enterpriseId, productId, request, "SELLER"));
        }
    }
}
