package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.CustomerRequest;
import com.procurement.dto.response.CustomerResponse;
import com.procurement.dto.response.PageResponse;
import com.procurement.entity.CrmCustomer;
import com.procurement.entity.OmsSalesOrder;
import com.procurement.mapper.CustomerMapper;
import com.procurement.mapper.SalesOrderMapper;
import com.procurement.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户管理服务实现
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final SalesOrderMapper salesOrderMapper;

    @Override
    public PageResponse<CustomerResponse> listByPage(Long enterpriseId, Integer pageNum,
                                                      Integer pageSize, String keyword) {
        LambdaQueryWrapper<CrmCustomer> wrapper = new LambdaQueryWrapper<CrmCustomer>()
                .eq(CrmCustomer::getEnterpriseId, enterpriseId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmCustomer::getName, keyword)
                    .or().like(CrmCustomer::getPhone, keyword));
        }
        wrapper.orderByDesc(CrmCustomer::getId);

        Page<CrmCustomer> page = customerMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<CustomerResponse> records = page.getRecords().stream()
                .map(c -> toResponse(c, enterpriseId)).toList();

        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public CustomerResponse getById(Long enterpriseId, Long id) {
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null || !customer.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toResponse(customer, enterpriseId);
    }

    @Override
    @Transactional
    public CustomerResponse create(Long enterpriseId, CustomerRequest request) {
        CrmCustomer customer = new CrmCustomer();
        customer.setEnterpriseId(enterpriseId);
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setRemark(request.getRemark());
        customerMapper.insert(customer);
        return toResponse(customer, enterpriseId);
    }

    @Override
    @Transactional
    public CustomerResponse update(Long enterpriseId, Long id, CustomerRequest request) {
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null || !customer.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setRemark(request.getRemark());
        customerMapper.updateById(customer);
        return toResponse(customer, enterpriseId);
    }

    @Override
    @Transactional
    public void delete(Long enterpriseId, Long id) {
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null || !customer.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        customerMapper.deleteById(id);
    }

    /**
     * Entity → Response DTO（含统计字段）
     */
    private CustomerResponse toResponse(CrmCustomer customer, Long enterpriseId) {
        CustomerResponse resp = new CustomerResponse();
        resp.setId(customer.getId());
        resp.setName(customer.getName());
        resp.setPhone(customer.getPhone());
        resp.setAddress(customer.getAddress());
        resp.setRemark(customer.getRemark());

        // 统计订单数量和总金额
        List<OmsSalesOrder> orders = salesOrderMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrder>()
                        .eq(OmsSalesOrder::getEnterpriseId, enterpriseId)
                        .eq(OmsSalesOrder::getCustomerId, customer.getId()));

        resp.setOrderCount(orders.size());
        resp.setTotalAmount(orders.stream()
                .map(OmsSalesOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return resp;
    }
}
