package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.PmsCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<PmsCategory> {
}
