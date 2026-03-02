package com.procurement.service;

import com.procurement.dto.request.CategoryRequest;
import com.procurement.entity.PmsCategory;

import java.util.List;
import java.util.Map;

/**
 * 商品分类服务接口
 */
public interface CategoryService {

    /**
     * 获取企业下的分类列表（含商品数量）
     */
    List<Map<String, Object>> listByEnterprise(Long enterpriseId);

    /**
     * 创建分类
     */
    PmsCategory create(Long enterpriseId, CategoryRequest request);

    /**
     * 更新分类
     */
    PmsCategory update(Long enterpriseId, Long id, CategoryRequest request);

    /**
     * 删除分类（分类下有商品时返回错误）
     */
    void delete(Long enterpriseId, Long id);

    /**
     * 批量更新排序
     */
    void batchSort(Long enterpriseId, List<Map<String, Object>> sortList);
}
