package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.StatDailyProductSales;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatDailyProductSalesMapper extends BaseMapper<StatDailyProductSales> {

    /**
     * 幂等写入/更新日聚合数据（INSERT ... ON DUPLICATE KEY UPDATE）
     */
    void insertOrUpdate(@Param("list") List<StatDailyProductSales> list);

    /**
     * 按时间范围查询商品排行 Top N（从摘要表聚合）
     */
    List<Map<String, Object>> selectRanking(
            @Param("enterpriseId") Long enterpriseId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("limit") int limit);
}
