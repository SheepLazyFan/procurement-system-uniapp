package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.OmsPurchaseOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PurchaseOrderMapper extends BaseMapper<OmsPurchaseOrder> {
}
