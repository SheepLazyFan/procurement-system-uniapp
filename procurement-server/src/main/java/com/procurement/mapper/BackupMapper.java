package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.SysBackup;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BackupMapper extends BaseMapper<SysBackup> {
}
