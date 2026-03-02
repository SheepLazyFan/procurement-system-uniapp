package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.OmsSalesOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SalesOrderItemMapper extends BaseMapper<OmsSalesOrderItem> {
}
