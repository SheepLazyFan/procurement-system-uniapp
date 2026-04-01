package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.OmsSalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface SalesOrderMapper extends BaseMapper<OmsSalesOrder> {

    /** 按客户分组统计订单数与总金额（用于客户列表） */
    List<Map<String, Object>> selectCustomerOrderStats(
            @Param("enterpriseId") Long enterpriseId,
            @Param("customerIds") Collection<Long> customerIds);

    /** 按时间段聚合销售总额与利润（概览用） */
    Map<String, Object> selectSalesOverview(
            @Param("enterpriseId") Long enterpriseId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /** 按日期分组统计销售趋势 */
    List<Map<String, Object>> selectSalesTrend(
            @Param("enterpriseId") Long enterpriseId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /** 商品销售排名（JOIN 明细表） */
    List<Map<String, Object>> selectProductRanking(
            @Param("enterpriseId") Long enterpriseId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("limit") int limit);

    /** 客户排名（JOIN 客户表） */
    List<Map<String, Object>> selectCustomerRanking(
            @Param("enterpriseId") Long enterpriseId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("limit") int limit);

    /** 查询指定客户的最近销售订单 */
    List<Map<String, Object>> selectRecentOrdersByCustomer(
            @Param("enterpriseId") Long enterpriseId,
            @Param("customerId") Long customerId,
            @Param("limit") int limit);
}
