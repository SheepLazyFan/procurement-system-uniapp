package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.SupplierRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.SupplierResponse;
import com.procurement.entity.CrmSupplier;
import com.procurement.entity.OmsPurchaseOrder;
import com.procurement.mapper.PurchaseOrderMapper;
import com.procurement.mapper.SupplierMapper;
import com.procurement.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 供应商管理服务实现
 */
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierMapper supplierMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;

    @Override
    public PageResponse<SupplierResponse> listByPage(Long enterpriseId, Integer pageNum,
                                                      Integer pageSize, String keyword) {
        LambdaQueryWrapper<CrmSupplier> wrapper = new LambdaQueryWrapper<CrmSupplier>()
                .eq(CrmSupplier::getEnterpriseId, enterpriseId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmSupplier::getName, keyword)
                    .or().like(CrmSupplier::getPhone, keyword));
        }
        wrapper.orderByDesc(CrmSupplier::getId);

        Page<CrmSupplier> page = supplierMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<SupplierResponse> records = page.getRecords().stream()
                .map(s -> toResponse(s, enterpriseId)).toList();

        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SupplierResponse getById(Long enterpriseId, Long id) {
        CrmSupplier supplier = supplierMapper.selectById(id);
        if (supplier == null || !supplier.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toResponse(supplier, enterpriseId);
    }

    @Override
    @Transactional
    public SupplierResponse create(Long enterpriseId, SupplierRequest request) {
        CrmSupplier supplier = new CrmSupplier();
        supplier.setEnterpriseId(enterpriseId);
        supplier.setName(request.getName());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setMainCategory(request.getMainCategory());
        supplier.setRemark(request.getRemark());
        supplierMapper.insert(supplier);
        return toResponse(supplier, enterpriseId);
    }

    @Override
    @Transactional
    public SupplierResponse update(Long enterpriseId, Long id, SupplierRequest request) {
        CrmSupplier supplier = supplierMapper.selectById(id);
        if (supplier == null || !supplier.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        supplier.setName(request.getName());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setMainCategory(request.getMainCategory());
        supplier.setRemark(request.getRemark());
        supplierMapper.updateById(supplier);
        return toResponse(supplier, enterpriseId);
    }

    @Override
    @Transactional
    public void delete(Long enterpriseId, Long id) {
        CrmSupplier supplier = supplierMapper.selectById(id);
        if (supplier == null || !supplier.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        supplierMapper.deleteById(id);
    }

    /**
     * Entity → Response DTO（含统计字段）
     */
    private SupplierResponse toResponse(CrmSupplier supplier, Long enterpriseId) {
        SupplierResponse resp = new SupplierResponse();
        resp.setId(supplier.getId());
        resp.setName(supplier.getName());
        resp.setPhone(supplier.getPhone());
        resp.setAddress(supplier.getAddress());
        resp.setMainCategory(supplier.getMainCategory());
        resp.setRemark(supplier.getRemark());

        // 统计采购次数和金额
        List<OmsPurchaseOrder> orders = purchaseOrderMapper.selectList(
                new LambdaQueryWrapper<OmsPurchaseOrder>()
                        .eq(OmsPurchaseOrder::getEnterpriseId, enterpriseId)
                        .eq(OmsPurchaseOrder::getSupplierId, supplier.getId()));

        resp.setPurchaseCount(orders.size());
        resp.setTotalAmount(orders.stream()
                .map(OmsPurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return resp;
    }
}
