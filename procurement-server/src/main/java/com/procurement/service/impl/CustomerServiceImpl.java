package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.CustomerRequest;
import com.procurement.dto.response.CustomerResponse;
import com.procurement.dto.response.PageResponse;
import com.procurement.entity.CrmCustomer;
import com.procurement.mapper.CustomerMapper;
import com.procurement.mapper.SalesOrderMapper;
import com.procurement.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

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

        List<CrmCustomer> customers = page.getRecords();
        if (customers.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), page.getTotal(), pageNum, pageSize);
        }

        // SQL 聚合：一次查询所有客户的订单统计
        Set<Long> customerIds = new HashSet<>();
        customers.forEach(c -> customerIds.add(c.getId()));
        Map<Long, Map<String, Object>> statsMap = new HashMap<>();
        List<Map<String, Object>> statsList = salesOrderMapper.selectCustomerOrderStats(enterpriseId, customerIds);
        for (Map<String, Object> s : statsList) {
            statsMap.put(((Number) s.get("customerId")).longValue(), s);
        }

        List<CustomerResponse> records = customers.stream()
                .map(c -> toResponse(c, statsMap.get(c.getId()))).toList();

        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public CustomerResponse getById(Long enterpriseId, Long id) {
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null || !customer.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<Map<String, Object>> stats = salesOrderMapper.selectCustomerOrderStats(
                enterpriseId, Collections.singleton(id));
        CustomerResponse resp = toResponse(customer, stats.isEmpty() ? null : stats.get(0));

        // 填充最近订单记录
        List<Map<String, Object>> recentRows = salesOrderMapper.selectRecentOrdersByCustomer(
                enterpriseId, id, 10);
        List<CustomerResponse.RecentOrder> recentOrders = recentRows.stream().map(row -> {
            CustomerResponse.RecentOrder ro = new CustomerResponse.RecentOrder();
            ro.setId(((Number) row.get("id")).longValue());
            ro.setOrderNo((String) row.get("orderNo"));
            ro.setTotalAmount((BigDecimal) row.get("totalAmount"));
            ro.setStatus((String) row.get("status"));
            ro.setPaymentStatus((String) row.get("paymentStatus"));
            ro.setCreatedAt(row.get("createdAt") != null ? row.get("createdAt").toString() : null);
            return ro;
        }).toList();
        resp.setRecentOrders(recentOrders);

        return resp;
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
        return toResponse(customer, null);
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
        return toResponse(customer, null);
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
     * Entity → Response DTO（使用 SQL 聚合统计数据替代全表扫描）
     */
    private CustomerResponse toResponse(CrmCustomer customer, Map<String, Object> stats) {
        CustomerResponse resp = new CustomerResponse();
        resp.setId(customer.getId());
        resp.setName(customer.getName());
        resp.setPhone(customer.getPhone());
        resp.setAddress(customer.getAddress());
        resp.setRemark(customer.getRemark());

        if (stats != null) {
            resp.setOrderCount(((Number) stats.get("orderCount")).intValue());
            resp.setTotalAmount((BigDecimal) stats.get("totalAmount"));
        } else {
            resp.setOrderCount(0);
            resp.setTotalAmount(BigDecimal.ZERO);
        }

        return resp;
    }
}
