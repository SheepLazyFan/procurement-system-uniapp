package com.procurement.service;

import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.CustomerRequest;
import com.procurement.entity.CrmCustomer;
import com.procurement.mapper.CustomerMapper;
import com.procurement.mapper.SalesOrderMapper;
import com.procurement.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CustomerServiceImpl 单元测试
 * <p>
 * 测试范围：客户 CRUD、企业数据隔离、删除守卫。
 * 依赖全部 Mock，不启动 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl — 客户管理服务")
class CustomerServiceImplTest {

    @Mock private CustomerMapper customerMapper;
    @Mock private SalesOrderMapper salesOrderMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    // ===========================================================
    // 辅助方法
    // ===========================================================

    private CrmCustomer buildCustomer(Long id, Long enterpriseId, String name) {
        CrmCustomer c = new CrmCustomer();
        c.setId(id);
        c.setEnterpriseId(enterpriseId);
        c.setName(name);
        c.setPhone("13800138000");
        return c;
    }

    private CustomerRequest buildRequest(String name, String phone) {
        CustomerRequest req = new CustomerRequest();
        req.setName(name);
        req.setPhone(phone);
        return req;
    }

    // ===========================================================
    // 1. create — 创建客户
    // ===========================================================

    @Test
    @DisplayName("Should create customer with enterprise binding")
    void should_createCustomer_withEnterpriseBinding() {
        // Arrange
        CustomerRequest request = buildRequest("张老板", "13900139000");

        // Act
        customerService.create(1L, request);

        // Assert
        ArgumentCaptor<CrmCustomer> captor = ArgumentCaptor.forClass(CrmCustomer.class);
        verify(customerMapper).insert(captor.capture());
        assertThat(captor.getValue().getEnterpriseId()).isEqualTo(1L);
        assertThat(captor.getValue().getName()).isEqualTo("张老板");
        assertThat(captor.getValue().getPhone()).isEqualTo("13900139000");
    }

    // ===========================================================
    // 2. getById — 企业数据隔离
    // ===========================================================

    @Nested
    @DisplayName("getById — 数据隔离")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw NOT_FOUND when accessing another enterprise's customer")
        void should_throwNotFound_when_accessingOtherEnterpriseCustomer() {
            // Arrange
            CrmCustomer customer = buildCustomer(10L, 999L, "别人的客户");
            when(customerMapper.selectById(10L)).thenReturn(customer);

            // Act & Assert
            assertThatThrownBy(() -> customerService.getById(1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when customer does not exist")
        void should_throwNotFound_when_customerDoesNotExist() {
            // Arrange
            when(customerMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> customerService.getById(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }
    }

    // ===========================================================
    // 3. update — 更新客户
    // ===========================================================

    @Test
    @DisplayName("Should throw NOT_FOUND when updating another enterprise's customer")
    void should_throwNotFound_when_updatingOtherEnterpriseCustomer() {
        // Arrange
        CrmCustomer customer = buildCustomer(20L, 999L, "其他的客户");
        when(customerMapper.selectById(20L)).thenReturn(customer);

        // Act & Assert
        assertThatThrownBy(() -> customerService.update(1L, 20L, buildRequest("新名", "13800138000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    // ===========================================================
    // 4. delete — 删除客户
    // ===========================================================

    @Nested
    @DisplayName("delete — 删除")
    class DeleteTests {

        @Test
        @DisplayName("Should delete customer when it belongs to current enterprise")
        void should_deleteCustomer_when_belongsToEnterprise() {
            // Arrange
            CrmCustomer customer = buildCustomer(30L, 1L, "要删的客户");
            when(customerMapper.selectById(30L)).thenReturn(customer);

            // Act
            customerService.delete(1L, 30L);

            // Assert
            verify(customerMapper).deleteById(30L);
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when deleting another enterprise's customer")
        void should_throwNotFound_when_deletingOtherEnterpriseCustomer() {
            // Arrange
            CrmCustomer customer = buildCustomer(31L, 999L, "不是你的客户");
            when(customerMapper.selectById(31L)).thenReturn(customer);

            // Act & Assert
            assertThatThrownBy(() -> customerService.delete(1L, 31L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }
    }
}
