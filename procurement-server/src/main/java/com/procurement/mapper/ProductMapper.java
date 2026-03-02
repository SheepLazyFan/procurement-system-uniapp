package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.PmsProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<PmsProduct> {

    /**
     * 原子化库存调整（防止并发超卖）
     * @param productId 商品ID
     * @param quantity  调整量（正数=入库，负数=出库）
     * @return 影响行数
     */
    @Update("UPDATE pms_product SET stock = stock + #{quantity}, updated_at = NOW() " +
            "WHERE id = #{productId} AND is_deleted = 0 AND stock + #{quantity} >= 0")
    int adjustStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
