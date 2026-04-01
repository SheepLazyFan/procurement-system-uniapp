package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.OmsPurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface PurchaseOrderMapper extends BaseMapper<OmsPurchaseOrder> {

    /** 按供应商分组统计采购次数与总金额 */
    List<Map<String, Object>> selectSupplierOrderStats(
            @Param("enterpriseId") Long enterpriseId,
            @Param("supplierIds") Collection<Long> supplierIds);

    /** 查询指定供应商的最近采购订单 */
    List<Map<String, Object>> selectRecentOrdersBySupplier(
            @Param("enterpriseId") Long enterpriseId,
            @Param("supplierId") Long supplierId,
            @Param("limit") int limit);

    /** 批量查询采购订单的商品明细（用于构建商品摘要） */
    List<Map<String, Object>> selectOrderItemsByOrderIds(
            @Param("orderIds") Collection<Long> orderIds);
}
