package com.procurement.service;

import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.SupplierRequest;
import com.procurement.entity.CrmSupplier;
import com.procurement.mapper.PurchaseOrderMapper;
import com.procurement.mapper.SupplierMapper;
import com.procurement.service.impl.SupplierServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SupplierServiceImpl 单元测试
 * <p>
 * 测试范围：供应商 CRUD、企业数据隔离。
 * 依赖全部 Mock，不启动 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierServiceImpl — 供应商管理服务")
class SupplierServiceImplTest {

    @Mock private SupplierMapper supplierMapper;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    // ===========================================================
    // 辅助方法
    // ===========================================================

    private CrmSupplier buildSupplier(Long id, Long enterpriseId, String name) {
        CrmSupplier s = new CrmSupplier();
        s.setId(id);
        s.setEnterpriseId(enterpriseId);
        s.setName(name);
        s.setPhone("13800138000");
        s.setMainCategory("生鲜");
        return s;
    }

    private SupplierRequest buildRequest(String name) {
        SupplierRequest req = new SupplierRequest();
        req.setName(name);
        req.setPhone("13900139000");
        req.setMainCategory("日用品");
        return req;
    }

    // ===========================================================
    // 1. create — 创建供应商
    // ===========================================================

    @Test
    @DisplayName("Should create supplier with enterprise binding and mainCategory")
    void should_createSupplier_withEnterpriseBinding() {
        // Arrange
        SupplierRequest request = buildRequest("广州食品批发部");

        // Act
        supplierService.create(1L, request);

        // Assert
        ArgumentCaptor<CrmSupplier> captor = ArgumentCaptor.forClass(CrmSupplier.class);
        verify(supplierMapper).insert(captor.capture());
        assertThat(captor.getValue().getEnterpriseId()).isEqualTo(1L);
        assertThat(captor.getValue().getName()).isEqualTo("广州食品批发部");
        assertThat(captor.getValue().getMainCategory()).isEqualTo("日用品");
    }

    // ===========================================================
    // 2. getById — 企业数据隔离
    // ===========================================================

    @Nested
    @DisplayName("getById — 数据隔离")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw NOT_FOUND when accessing another enterprise's supplier")
        void should_throwNotFound_when_accessingOtherEnterpriseSupplier() {
            // Arrange
            CrmSupplier supplier = buildSupplier(10L, 999L, "别人的供应商");
            when(supplierMapper.selectById(10L)).thenReturn(supplier);

            // Act & Assert
            assertThatThrownBy(() -> supplierService.getById(1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when supplier does not exist")
        void should_throwNotFound_when_supplierDoesNotExist() {
            // Arrange
            when(supplierMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> supplierService.getById(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }
    }

    // ===========================================================
    // 3. update — 更新供应商
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when updating another enterprise's supplier")
    void should_throwNotFound_when_updatingOtherEnterpriseSupplier() {
        // Arrange
        CrmSupplier supplier = buildSupplier(20L, 999L, "不是你的供应商");
        when(supplierMapper.selectById(20L)).thenReturn(supplier);

        // Act & Assert
        assertThatThrownBy(() -> supplierService.update(1L, 20L, buildRequest("新名")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Should update supplier fields when enterprise matches")
    void should_updateSupplierFields_when_enterpriseMatches() {
        // Arrange
        CrmSupplier supplier = buildSupplier(21L, 1L, "旧名");
        when(supplierMapper.selectById(21L)).thenReturn(supplier);

        SupplierRequest request = buildRequest("新供应商名");
        request.setAddress("广州天河区");
        request.setRemark("优质供应商");

        // Act
        supplierService.update(1L, 21L, request);

        // Assert
        verify(supplierMapper).updateById(argThat(s ->
                "新供应商名".equals(s.getName())
                        && "广州天河区".equals(s.getAddress())
                        && "优质供应商".equals(s.getRemark())));
    }

    // ===========================================================
    // 4. delete — 删除供应商
    // ===========================================================

    @Nested
    @DisplayName("delete — 删除")
    class DeleteTests {

        @Test
        @DisplayName("Should delete supplier when it belongs to current enterprise")
        void should_deleteSupplier_when_belongsToEnterprise() {
            // Arrange
            CrmSupplier supplier = buildSupplier(30L, 1L, "要删除的供应商");
            when(supplierMapper.selectById(30L)).thenReturn(supplier);

            // Act
            supplierService.delete(1L, 30L);

            // Assert
            verify(supplierMapper).deleteById(30L);
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when deleting another enterprise's supplier")
        void should_throwNotFound_when_deletingOtherEnterpriseSupplier() {
            // Arrange
            CrmSupplier supplier = buildSupplier(31L, 999L, "别人的");
            when(supplierMapper.selectById(31L)).thenReturn(supplier);

            // Act & Assert
            assertThatThrownBy(() -> supplierService.delete(1L, 31L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }
    }
}
