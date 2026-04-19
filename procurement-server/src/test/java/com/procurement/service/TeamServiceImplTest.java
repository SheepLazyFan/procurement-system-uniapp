package com.procurement.service;

import com.procurement.common.constant.UserConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.TeamPermissionRequest;
import com.procurement.entity.SysEnterprise;
import com.procurement.entity.SysTeamMember;
import com.procurement.entity.SysUser;
import com.procurement.mapper.EnterpriseMapper;
import com.procurement.mapper.TeamMemberMapper;
import com.procurement.mapper.UserMapper;
import com.procurement.service.impl.TeamServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamServiceImpl - 团队服务")
class TeamServiceImplTest {

    @Mock private TeamMemberMapper teamMemberMapper;
    @Mock private UserMapper userMapper;
    @Mock private EnterpriseMapper enterpriseMapper;

    @InjectMocks
    private TeamServiceImpl teamService;

    @Test
    @DisplayName("Should join enterprise by invite code and default to SALES member")
    void should_joinEnterpriseAndAssignSalesRole_when_inviteCodeValid() {
        // Arrange
        SysUser currentUser = new SysUser();
        currentUser.setId(101L);
        currentUser.setEnterpriseId(null);

        SysEnterprise enterprise = new SysEnterprise();
        enterprise.setId(11L);
        enterprise.setName("晨光批发部");
        enterprise.setInviteCode("JOIN66");

        SysUser updateUser = new SysUser();
        updateUser.setId(101L);

        when(userMapper.selectById(101L)).thenReturn(currentUser, updateUser);
        when(enterpriseMapper.selectOne(any())).thenReturn(enterprise);
        when(teamMemberMapper.selectCount(any())).thenReturn(0L);

        // Act
        Map<String, Object> result = teamService.joinByInviteCode(101L, "JOIN66");

        // Assert
        ArgumentCaptor<SysTeamMember> memberCaptor = ArgumentCaptor.forClass(SysTeamMember.class);
        verify(teamMemberMapper).insert(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getEnterpriseId()).isEqualTo(11L);
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(101L);
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(UserConstants.MEMBER_ROLE_SALES);

        verify(userMapper).updateById(argThat(user ->
                user.getId().equals(101L)
                        && user.getEnterpriseId().equals(11L)
                        && UserConstants.ROLE_MEMBER.equals(user.getRole())
                        && Integer.valueOf(1).equals(user.getNotifyStockWarning())));
        assertThat(result.get("enterpriseId")).isEqualTo(11L);
        assertThat(result.get("enterpriseName")).isEqualTo("晨光批发部");
    }

    @Test
    @DisplayName("Should reject joining another enterprise when user already bound")
    void should_rejectJoin_when_userAlreadyBoundToEnterprise() {
        // Arrange
        SysUser currentUser = new SysUser();
        currentUser.setId(102L);
        currentUser.setEnterpriseId(55L);
        when(userMapper.selectById(102L)).thenReturn(currentUser);

        // Act & Assert
        assertThatThrownBy(() -> teamService.joinByInviteCode(102L, "ANY666"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.CONFLICT.getCode()));
    }

    @Test
    @DisplayName("Should list owner first before team members")
    void should_listOwnerFirst_when_listingMembers() {
        // Arrange
        SysEnterprise enterprise = new SysEnterprise();
        enterprise.setId(20L);
        enterprise.setOwnerId(1L);
        enterprise.setCreatedAt(LocalDateTime.of(2026, 4, 7, 9, 0));

        SysUser owner = new SysUser();
        owner.setId(1L);
        owner.setNickName("店主");
        owner.setPhone("13800000001");

        SysTeamMember member = new SysTeamMember();
        member.setId(901L);
        member.setEnterpriseId(20L);
        member.setUserId(2L);
        member.setRole(UserConstants.MEMBER_ROLE_ADMIN);
        member.setPermissions(Map.of("inventory", true));
        member.setCreatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));

        SysUser adminUser = new SysUser();
        adminUser.setId(2L);
        adminUser.setNickName("管理员");
        adminUser.setPhone("13800000002");

        when(enterpriseMapper.selectById(20L)).thenReturn(enterprise);
        when(userMapper.selectById(1L)).thenReturn(owner);
        when(teamMemberMapper.selectList(any())).thenReturn(List.of(member));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(adminUser));

        // Act
        List<Map<String, Object>> members = teamService.listMembers(20L);

        // Assert
        assertThat(members).hasSize(2);
        assertThat(members.get(0).get("role")).isEqualTo(UserConstants.ROLE_SELLER);
        assertThat(members.get(0).get("nickName")).isEqualTo("店主");
        assertThat(members.get(1).get("role")).isEqualTo(UserConstants.MEMBER_ROLE_ADMIN);
        assertThat(members.get(1).get("permissions")).isEqualTo(Map.of("inventory", true));
    }

    @Test
    @DisplayName("Should reject changing owner permissions")
    void should_rejectPermissionChange_when_targetIsOwner() {
        // Arrange
        SysTeamMember member = new SysTeamMember();
        member.setId(777L);
        member.setEnterpriseId(20L);
        member.setUserId(1L);

        SysEnterprise enterprise = new SysEnterprise();
        enterprise.setId(20L);
        enterprise.setOwnerId(1L);

        TeamPermissionRequest request = new TeamPermissionRequest();
        request.setRole(UserConstants.MEMBER_ROLE_WAREHOUSE);

        when(teamMemberMapper.selectById(777L)).thenReturn(member);
        when(enterpriseMapper.selectById(20L)).thenReturn(enterprise);

        // Act & Assert
        assertThatThrownBy(() -> teamService.setPermissions(20L, 777L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.FORBIDDEN.getCode()));
    }

    @Test
    @DisplayName("Should remove member and clear enterprise binding")
    void should_removeMemberAndClearUserBinding_when_memberExists() {
        // Arrange
        SysTeamMember member = new SysTeamMember();
        member.setId(888L);
        member.setEnterpriseId(20L);
        member.setUserId(3L);

        SysEnterprise enterprise = new SysEnterprise();
        enterprise.setId(20L);
        enterprise.setOwnerId(1L);

        SysUser user = new SysUser();
        user.setId(3L);
        user.setEnterpriseId(20L);
        user.setRole(UserConstants.ROLE_MEMBER);

        when(teamMemberMapper.selectById(888L)).thenReturn(member);
        when(enterpriseMapper.selectById(20L)).thenReturn(enterprise);
        when(userMapper.selectById(3L)).thenReturn(user);

        // Act
        teamService.removeMember(20L, 888L);

        // Assert
        verify(teamMemberMapper).deleteById(888L);
        verify(userMapper).updateById(argThat(item ->
                item.getId().equals(3L)
                        && item.getEnterpriseId() == null
                        && UserConstants.ROLE_MEMBER.equals(item.getRole())));
    }
}
