package com.procurement.service;

import com.procurement.dto.request.TeamPermissionRequest;

import java.util.List;
import java.util.Map;

/**
 * 团队管理服务接口
 */
public interface TeamService {

    /**
     * 获取团队成员列表
     */
    List<Map<String, Object>> listMembers(Long enterpriseId);

    /**
     * 通过邀请码加入团队
     */
    Map<String, Object> joinByInviteCode(Long userId, String inviteCode);

    /**
     * 设置成员权限
     */
    void setPermissions(Long enterpriseId, Long memberId, TeamPermissionRequest request);

    /**
     * 移除成员
     */
    void removeMember(Long enterpriseId, Long memberId);
}
