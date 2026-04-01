package com.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.procurement.entity.PmsProductSupplier;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductSupplierMapper extends BaseMapper<PmsProductSupplier> {

    /**
     * 查询供应商关联商品详情列表（联查商品表、分类表），支持关键词和分类过滤，带供应商数量
     */
    List<Map<String, Object>> selectLinkedProducts(
            @Param("enterpriseId") Long enterpriseId,
            @Param("supplierId") Long supplierId,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 统计供应商关联商品数（用于分页 total）
     */
    long countLinkedProducts(
            @Param("enterpriseId") Long enterpriseId,
            @Param("supplierId") Long supplierId,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId);

    /**
     * 统计一个商品绑定了多少个供应商（用于解绑前的友好提示）
     */
    int countSuppliersByProduct(
            @Param("enterpriseId") Long enterpriseId,
            @Param("productId") Long productId);

    /**
     * 物理删除——绕过 @TableLogic 软删除，确保 UNIQUE KEY 可被重复绑定
     */
    @Delete("DELETE FROM pms_product_supplier " +
            "WHERE enterprise_id = #{enterpriseId} AND supplier_id = #{supplierId} AND product_id = #{productId}")
    int physicalDelete(
            @Param("enterpriseId") Long enterpriseId,
            @Param("supplierId") Long supplierId,
            @Param("productId") Long productId);
}
