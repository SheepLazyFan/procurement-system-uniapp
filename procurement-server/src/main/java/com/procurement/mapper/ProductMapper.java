package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.PmsProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper extends BaseMapper<PmsProduct> {

    /**
     * 原子化库存调整（防止并发超卖）
     */
    @Update("UPDATE pms_product SET stock = stock + #{quantity}, updated_at = NOW() " +
            "WHERE id = #{productId} AND is_deleted = 0 AND stock + #{quantity} >= 0")
    int adjustStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 按分类统计上架商品数，支持库存状态 / 价格区间过滤，结果供买家端分类标签徽章使用。
     * 未分配分类（category_id IS NULL）的商品不统计。SQL 定义见 ProductMapper.xml。
     */
    List<Map<String, Object>> countByCategoryIdFiltered(
            @Param("enterpriseId") Long enterpriseId,
            @Param("stockStatus") String stockStatus,
            @Param("priceMin") java.math.BigDecimal priceMin,
            @Param("priceMax") java.math.BigDecimal priceMax);
}
