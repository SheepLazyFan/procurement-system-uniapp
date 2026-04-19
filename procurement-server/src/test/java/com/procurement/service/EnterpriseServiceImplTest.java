package com.procurement.service;

import com.procurement.common.constant.UserConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.EnterpriseRequest;
import com.procurement.dto.response.EnterpriseResponse;
import com.procurement.entity.SysEnterprise;
import com.procurement.entity.SysUser;
import com.procurement.mapper.EnterpriseMapper;
import com.procurement.mapper.UserMapper;
import com.procurement.service.impl.EnterpriseServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnterpriseServiceImpl - 企业服务")
class EnterpriseServiceImplTest {

    @Mock private EnterpriseMapper enterpriseMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private EnterpriseServiceImpl enterpriseService;

    @Test
    @DisplayName("Should create enterprise and promote owner to SELLER")
    void should_createEnterpriseAndPromoteOwner_when_firstCreate() {
        // Arrange
        EnterpriseRequest request = new EnterpriseRequest();
        request.setName("晨光批发部");
        request.setAddress("深圳市龙岗区");
        request.setContactName("张老板");
        request.setContactPhone("13800138000");

        SysUser owner = new SysUser();
        owner.setId(100L);
        owner.setRole(UserConstants.ROLE_MEMBER);

        when(enterpriseMapper.selectOne(any())).thenReturn(null);
        when(enterpriseMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(100L)).thenReturn(owner);
        doAnswer(invocation -> {
            SysEnterprise enterprise = invocation.getArgument(0);
            enterprise.setId(200L);
            return 1;
        }).when(enterpriseMapper).insert(any(SysEnterprise.class));

        // Act
        EnterpriseResponse response = enterpriseService.create(100L, request);

        // Assert
        verify(enterpriseMapper).insert(argThat(enterprise ->
                "晨光批发部".equals(enterprise.getName())
                        && enterprise.getOwnerId().equals(100L)
                        && enterprise.getInviteCode() != null
                        && !enterprise.getInviteCode().isBlank()));
        verify(userMapper).updateById(argThat(user ->
                user.getId().equals(100L)
                        && user.getEnterpriseId().equals(200L)
                        && UserConstants.ROLE_SELLER.equals(user.getRole())));
        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getName()).isEqualTo("晨光批发部");
    }

    @Test
    @DisplayName("Should throw ENTERPRISE_ALREADY_EXISTS when owner already has enterprise")
    void should_throwEnterpriseAlreadyExists_when_ownerAlreadyCreated() {
        // Arrange
        SysEnterprise existing = new SysEnterprise();
        existing.setId(1L);
        existing.setOwnerId(100L);
        when(enterpriseMapper.selectOne(any())).thenReturn(existing);

        EnterpriseRequest request = new EnterpriseRequest();
        request.setName("重复企业");

        // Act & Assert
        assertThatThrownBy(() -> enterpriseService.create(100L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ENTERPRISE_ALREADY_EXISTS.getCode()));
    }

    @Test
    @DisplayName("Should update enterprise profile and keep existing media when request omits urls")
    void should_updateEnterpriseAndKeepExistingMedia_when_urlsNotProvided() {
        // Arrange
        SysEnterprise enterprise = new SysEnterprise();
        enterprise.setId(12L);
        enterprise.setName("旧企业");
        enterprise.setAddress("旧地址");
        enterprise.setContactName("旧联系人");
        enterprise.setContactPhone("10086");
        enterprise.setLogoUrl("https://cdn.example.com/logo-old.png");
        enterprise.setPaymentQrUrl("https://cdn.example.com/pay-old.png");

        EnterpriseRequest request = new EnterpriseRequest();
        request.setName("新企业");
        request.setAddress("新地址");
        request.setContactName("新联系人");
        request.setContactPhone("13800138000");
        request.setLogoUrl(null);
        request.setPaymentQrUrl(null);

        when(enterpriseMapper.selectById(12L)).thenReturn(enterprise);

        // Act
        EnterpriseResponse response = enterpriseService.update(12L, request);

        // Assert
        verify(enterpriseMapper).updateById(argThat(item ->
                item.getId().equals(12L)
                        && "新企业".equals(item.getName())
                        && "https://cdn.example.com/logo-old.png".equals(item.getLogoUrl())
                        && "https://cdn.example.com/pay-old.png".equals(item.getPaymentQrUrl())));
        assertThat(response.getName()).isEqualTo("新企业");
        assertThat(response.getLogoUrl()).isEqualTo("https://cdn.example.com/logo-old.png");
        assertThat(response.getPaymentQrUrl()).isEqualTo("https://cdn.example.com/pay-old.png");
    }

    @Test
    @DisplayName("Should refresh invite code when enterprise exists")
    void should_refreshInviteCode_when_enterpriseExists() {
        // Arrange
        SysEnterprise enterprise = new SysEnterprise();
        enterprise.setId(88L);
        enterprise.setInviteCode("ABC123");
        when(enterpriseMapper.selectById(88L)).thenReturn(enterprise);
        when(enterpriseMapper.selectCount(any())).thenReturn(0L);

        // Act
        String newCode = enterpriseService.refreshInviteCode(88L);

        // Assert
        assertThat(newCode).isNotBlank();
        assertThat(newCode).isNotEqualTo("ABC123");
        verify(enterpriseMapper).updateById(argThat(item ->
                item.getId().equals(88L)
                        && item.getInviteCode() != null
                        && item.getInviteCode().equals(newCode)));
    }

    @Test
    @DisplayName("Should throw ENTERPRISE_NOT_FOUND when user enterprise binding is stale")
    void should_throwEnterpriseNotFound_when_userBindingIsStale() {
        // Arrange
        SysUser user = new SysUser();
        user.setId(77L);
        user.setEnterpriseId(999L);
        when(userMapper.selectById(77L)).thenReturn(user);
        when(enterpriseMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> enterpriseService.getByUser(77L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ENTERPRISE_NOT_FOUND.getCode()));
    }
}
