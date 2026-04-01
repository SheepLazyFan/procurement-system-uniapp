package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.EnterpriseRequest;
import com.procurement.dto.response.EnterpriseResponse;
import com.procurement.entity.SysEnterprise;
import com.procurement.entity.SysUser;
import com.procurement.mapper.EnterpriseMapper;
import com.procurement.mapper.UserMapper;
import com.procurement.common.constant.UserConstants;
import com.procurement.service.EnterpriseService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 企业管理服务实现
 */
@Service
@RequiredArgsConstructor
public class EnterpriseServiceImpl implements EnterpriseService {

    private final EnterpriseMapper enterpriseMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public EnterpriseResponse create(Long userId, EnterpriseRequest request) {
        // 检查用户是否已创建企业
        SysEnterprise existing = enterpriseMapper.selectOne(
                new LambdaQueryWrapper<SysEnterprise>().eq(SysEnterprise::getOwnerId, userId));
        if (existing != null) {
            throw new BusinessException(ResultCode.ENTERPRISE_ALREADY_EXISTS);
        }

        // 创建企业
        SysEnterprise enterprise = new SysEnterprise();
        enterprise.setName(request.getName());
        enterprise.setAddress(request.getAddress());
        enterprise.setContactPhone(request.getContactPhone());
        enterprise.setContactName(request.getContactName());
        enterprise.setOwnerId(userId);
        enterprise.setInviteCode(generateUniqueInviteCode());
        try {
            enterpriseMapper.insert(enterprise);
        } catch (DuplicateKeyException e) {
            // owner_id 唯一索引冲突（并发创建）
            throw new BusinessException(ResultCode.ENTERPRISE_ALREADY_EXISTS);
        }

        // 更新用户的 enterpriseId，并确认身份为店主
        SysUser user = userMapper.selectById(userId);
        user.setEnterpriseId(enterprise.getId());
        user.setRole(UserConstants.ROLE_SELLER);
        userMapper.updateById(user);

        return toResponse(enterprise);
    }

    @Override
    public EnterpriseResponse getByUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getEnterpriseId() == null) {
            return null;
        }

        SysEnterprise enterprise = enterpriseMapper.selectById(user.getEnterpriseId());
        if (enterprise == null) {
            throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND);
        }
        return toResponse(enterprise);
    }

    @Override
    @Transactional
    public EnterpriseResponse update(Long enterpriseId, EnterpriseRequest request) {
        SysEnterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND);
        }

        enterprise.setName(request.getName());
        enterprise.setAddress(request.getAddress());
        enterprise.setContactPhone(request.getContactPhone());
        enterprise.setContactName(request.getContactName());
        if (request.getPaymentQrUrl() != null) {
            enterprise.setPaymentQrUrl(request.getPaymentQrUrl());
        }
        if (request.getLogoUrl() != null) {
            enterprise.setLogoUrl(request.getLogoUrl());
        }
        enterpriseMapper.updateById(enterprise);

        return toResponse(enterprise);
    }

    @Override
    @Transactional
    public String refreshInviteCode(Long enterpriseId) {
        SysEnterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND);
        }

        String newCode = generateUniqueInviteCode();
        enterprise.setInviteCode(newCode);
        enterpriseMapper.updateById(enterprise);
        return newCode;
    }

    /**
     * 生成唯一的6位邀请码（碰撞重试）
     */
    private String generateUniqueInviteCode() {
        for (int i = 0; i < 10; i++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            Long count = enterpriseMapper.selectCount(
                    new LambdaQueryWrapper<SysEnterprise>().eq(SysEnterprise::getInviteCode, code));
            if (count == 0) {
                return code;
            }
        }
        // 10次仍碰撞，使用8位
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * Entity → Response DTO
     */
    private EnterpriseResponse toResponse(SysEnterprise entity) {
        EnterpriseResponse resp = new EnterpriseResponse();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setAddress(entity.getAddress());
        resp.setContactPhone(entity.getContactPhone());
        resp.setContactName(entity.getContactName());
        resp.setInviteCode(entity.getInviteCode());
        resp.setLogoUrl(entity.getLogoUrl());
        resp.setPaymentQrUrl(entity.getPaymentQrUrl());
        return resp;
    }
}
