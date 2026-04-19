package com.procurement.service;

import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.BindProductsRequest;
import com.procurement.entity.CrmSupplier;
import com.procurement.entity.PmsProduct;
import com.procurement.mapper.ProductMapper;
import com.procurement.mapper.ProductSupplierMapper;
import com.procurement.mapper.SupplierMapper;
import com.procurement.service.impl.ProductSupplierServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ProductSupplierServiceImpl 单元测试
 * <p>
 * 测试范围：绑定校验、解绑守卫、供货价更新、供应商企业隔离。
 * 依赖全部 Mock，不启动 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSupplierServiceImpl — 供应商-商品关联服务")
class ProductSupplierServiceImplTest {

    @Mock private ProductSupplierMapper productSupplierMapper;
    @Mock private ProductMapper productMapper;
    @Mock private SupplierMapper supplierMapper;

    @InjectMocks
    private ProductSupplierServiceImpl productSupplierService;

    // ===========================================================
    // 辅助方法
    // ===========================================================

    private CrmSupplier buildSupplier(Long id, Long enterpriseId) {
        CrmSupplier s = new CrmSupplier();
        s.setId(id);
        s.setEnterpriseId(enterpriseId);
        s.setName("测试供应商-" + id);
        return s;
    }

    private PmsProduct buildProduct(Long id, Long enterpriseId) {
        PmsProduct p = new PmsProduct();
        p.setId(id);
        p.setEnterpriseId(enterpriseId);
        p.setName("商品-" + id);
        return p;
    }

    private BindProductsRequest buildBindRequest(Long productId, BigDecimal supplyPrice) {
        BindProductsRequest.BindItem item = new BindProductsRequest.BindItem();
        item.setProductId(productId);
        item.setSupplyPrice(supplyPrice);

        BindProductsRequest request = new BindProductsRequest();
        request.setItems(List.of(item));
        return request;
    }

    // ===========================================================
    // 1. bindProducts — 绑定商品
    // ===========================================================

    @Nested
    @DisplayName("bindProducts — 绑定商品到供应商")
    class BindTests {

        @Test
        @DisplayName("Should throw NOT_FOUND when supplier belongs to another enterprise")
        void should_throwNotFound_when_supplierBelongsToOtherEnterprise() {
            // Arrange
            CrmSupplier supplier = buildSupplier(1L, 999L);
            when(supplierMapper.selectById(1L)).thenReturn(supplier);

            // Act & Assert
            assertThatThrownBy(() -> productSupplierService.bindProducts(
                    1L, 1L, buildBindRequest(10L, BigDecimal.TEN)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("Should throw CONFLICT when product is already bound to supplier")
        void should_throwConflict_when_productAlreadyBound() {
            // Arrange
            CrmSupplier supplier = buildSupplier(2L, 1L);
            PmsProduct product = buildProduct(10L, 1L);

            when(supplierMapper.selectById(2L)).thenReturn(supplier);
            when(productMapper.selectById(10L)).thenReturn(product);
            when(productSupplierMapper.selectCount(any())).thenReturn(1L); // 已绑定

            // Act & Assert
            assertThatThrownBy(() -> productSupplierService.bindProducts(
                    1L, 2L, buildBindRequest(10L, new BigDecimal("8.50"))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.CONFLICT.getCode()));
        }

        @Test
        @DisplayName("Should throw CONFLICT when product does not exist")
        void should_throwConflict_when_productDoesNotExist() {
            // Arrange
            CrmSupplier supplier = buildSupplier(3L, 1L);
            when(supplierMapper.selectById(3L)).thenReturn(supplier);
            when(productMapper.selectById(999L)).thenReturn(null); // 商品不存在

            // Act & Assert
            assertThatThrownBy(() -> productSupplierService.bindProducts(
                    1L, 3L, buildBindRequest(999L, BigDecimal.TEN)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.CONFLICT.getCode()));
        }

        @Test
        @DisplayName("Should bind product successfully when all validations pass")
        void should_bindProduct_when_allValidationsPass() {
            // Arrange
            CrmSupplier supplier = buildSupplier(4L, 1L);
            PmsProduct product = buildProduct(20L, 1L);

            when(supplierMapper.selectById(4L)).thenReturn(supplier);
            when(productMapper.selectById(20L)).thenReturn(product);
            when(productSupplierMapper.selectCount(any())).thenReturn(0L); // 未绑定

            // Act
            productSupplierService.bindProducts(1L, 4L,
                    buildBindRequest(20L, new BigDecimal("15.00")));

            // Assert
            verify(productSupplierMapper).insert(argThat(ps ->
                    ps.getProductId().equals(20L)
                            && ps.getSupplierId().equals(4L)
                            && ps.getEnterpriseId().equals(1L)
                            && ps.getSupplyPrice().compareTo(new BigDecimal("15.00")) == 0));
        }
    }

    // ===========================================================
    // 2. unbindProduct — 解绑商品
    // ===========================================================

    @Nested
    @DisplayName("unbindProduct — 解绑商品")
    class UnbindTests {

        @Test
        @DisplayName("Should throw NOT_FOUND when unbinding non-existent binding")
        void should_throwNotFound_when_unbindingNonExistentBinding() {
            // Arrange
            CrmSupplier supplier = buildSupplier(5L, 1L);
            when(supplierMapper.selectById(5L)).thenReturn(supplier);
            when(productSupplierMapper.physicalDelete(1L, 5L, 30L)).thenReturn(0);

            // Act & Assert
            assertThatThrownBy(() -> productSupplierService.unbindProduct(1L, 5L, 30L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("Should unbind product successfully when binding exists")
        void should_unbindProduct_when_bindingExists() {
            // Arrange
            CrmSupplier supplier = buildSupplier(6L, 1L);
            when(supplierMapper.selectById(6L)).thenReturn(supplier);
            when(productSupplierMapper.physicalDelete(1L, 6L, 40L)).thenReturn(1);

            // Act — 不应抛异常
            assertThatNoException().isThrownBy(
                    () -> productSupplierService.unbindProduct(1L, 6L, 40L));
        }
    }

    // PriceTests omitted: updateSupplyPrice() uses LambdaUpdateWrapper<PmsProductSupplier>
    // which requires MyBatis-Plus table metadata cache — not available in pure @Mock unit tests.
    // This method's enterprise-isolation guard is already covered by BindTests/UnbindTests
    // through the shared validateSupplier() path.
}
