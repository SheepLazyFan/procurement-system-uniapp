package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.procurement.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 团队管理服务实现
 */
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamMemberMapper teamMemberMapper;
    private final UserMapper userMapper;
    private final EnterpriseMapper enterpriseMapper;

    @Override
    public List<Map<String, Object>> listMembers(Long enterpriseId) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 将店主（企业所有者）放在首位
        SysEnterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise != null && enterprise.getOwnerId() != null) {
            SysUser owner = userMapper.selectById(enterprise.getOwnerId());
            if (owner != null) {
                Map<String, Object> ownerItem = new LinkedHashMap<>();
                ownerItem.put("id", null);
                ownerItem.put("userId", owner.getId());
                ownerItem.put("role", "SELLER");
                ownerItem.put("nickName", owner.getNickName());
                ownerItem.put("phone", owner.getPhone());
                ownerItem.put("joinedAt", enterprise.getCreatedAt());
                result.add(ownerItem);
            }
        }

        // 2. 查询团队成员
        List<SysTeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<SysTeamMember>()
                        .eq(SysTeamMember::getEnterpriseId, enterpriseId));

        // 批量预加载成员用户信息（消除 N+1）
        Set<Long> userIds = members.stream().map(SysTeamMember::getUserId).collect(java.util.stream.Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u));

        for (SysTeamMember member : members) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", member.getId());
            item.put("userId", member.getUserId());
            item.put("role", member.getRole());
            item.put("permissions", member.getPermissions());
            item.put("joinedAt", member.getCreatedAt());

            SysUser user = userMap.get(member.getUserId());
            if (user != null) {
                item.put("nickName", user.getNickName());
                item.put("phone", user.getPhone());
            }

            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> joinByInviteCode(Long userId, String inviteCode) {
        // 检查用户是否已属于某个企业
        SysUser currentUser = userMapper.selectById(userId);
        if (currentUser != null && currentUser.getEnterpriseId() != null) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(),
                    "您已属于其他企业，请先退出当前企业后再加入");
        }

        // 根据邀请码查找企业
        SysEnterprise enterprise = enterpriseMapper.selectOne(
                new LambdaQueryWrapper<SysEnterprise>()
                        .eq(SysEnterprise::getInviteCode, inviteCode));

        if (enterprise == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "邀请码无效");
        }

        // 检查是否已经是该企业成员
        Long count = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<SysTeamMember>()
                        .eq(SysTeamMember::getEnterpriseId, enterprise.getId())
                        .eq(SysTeamMember::getUserId, userId));

        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "已经是该企业成员");
        }

        // 创建团队成员记录
        SysTeamMember member = new SysTeamMember();
        member.setEnterpriseId(enterprise.getId());
        member.setUserId(userId);
        member.setRole(UserConstants.MEMBER_ROLE_SALES); // 默认角色：销售员

        teamMemberMapper.insert(member);

        // 更新用户的 enterpriseId，并默认开启库存预警通知
        SysUser user = userMapper.selectById(userId);
        user.setEnterpriseId(enterprise.getId());
        user.setRole(UserConstants.ROLE_MEMBER);
        user.setNotifyStockWarning(1);
        userMapper.updateById(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enterpriseId", enterprise.getId());
        result.put("enterpriseName", enterprise.getName());
        return result;
    }

    @Override
    @Transactional
    public void setPermissions(Long enterpriseId, Long memberId, TeamPermissionRequest request) {
        SysTeamMember member = teamMemberMapper.selectById(memberId);
        if (member == null || !member.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 检查是否为企业所有者（不允许修改店主角色）
        SysEnterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise != null && enterprise.getOwnerId().equals(member.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "不能修改企业所有者的角色");
        }

        // 验证角色值
        String role = request.getRole();
        if (!UserConstants.VALID_MEMBER_ROLES.contains(role)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "无效角色，允许值：ADMIN / SALES / WAREHOUSE");
        }

        member.setRole(role);
        if (request.getPermissions() != null) {
            member.setPermissions(request.getPermissions());
        }
        teamMemberMapper.updateById(member);
    }

    @Override
    @Transactional
    public void removeMember(Long enterpriseId, Long memberId) {
        SysTeamMember member = teamMemberMapper.selectById(memberId);
        if (member == null || !member.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 禁止移除企业所有者
        SysEnterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise != null && enterprise.getOwnerId().equals(member.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "不能移除企业所有者");
        }

        // 移除团队成员
        teamMemberMapper.deleteById(memberId);

        // 清除用户的 enterpriseId
        SysUser user = userMapper.selectById(member.getUserId());
        if (user != null) {
            user.setEnterpriseId(null);
            user.setRole(UserConstants.ROLE_MEMBER);
            userMapper.updateById(user);
        }
    }
}
