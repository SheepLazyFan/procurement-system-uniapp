package com.procurement.service;

import com.procurement.dto.request.EnterpriseRequest;
import com.procurement.dto.response.EnterpriseResponse;

/**
 * 企业管理服务接口
 */
public interface EnterpriseService {

    /**
     * 创建企业
     */
    EnterpriseResponse create(Long userId, EnterpriseRequest request);

    /**
     * 获取当前用户所属企业信息
     */
    EnterpriseResponse getByUser(Long userId);

    /**
     * 更新企业信息
     */
    EnterpriseResponse update(Long enterpriseId, EnterpriseRequest request);

    /**
     * 刷新邀请码
     */
    String refreshInviteCode(Long enterpriseId);
}
