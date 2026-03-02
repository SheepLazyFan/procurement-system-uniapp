package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.CategoryRequest;
import com.procurement.entity.PmsCategory;
import com.procurement.entity.PmsProduct;
import com.procurement.mapper.CategoryMapper;
import com.procurement.mapper.ProductMapper;
import com.procurement.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 商品分类服务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Override
    public List<Map<String, Object>> listByEnterprise(Long enterpriseId) {
        // 查询该企业下的所有分类
        List<PmsCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<PmsCategory>()
                        .eq(PmsCategory::getEnterpriseId, enterpriseId)
                        .orderByAsc(PmsCategory::getSortOrder)
                        .orderByAsc(PmsCategory::getId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (PmsCategory cat : categories) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", cat.getId());
            item.put("name", cat.getName());
            item.put("sortOrder", cat.getSortOrder());

            // 查询分类下的商品数量
            Long productCount = productMapper.selectCount(
                    new LambdaQueryWrapper<PmsProduct>()
                            .eq(PmsProduct::getEnterpriseId, enterpriseId)
                            .eq(PmsProduct::getCategoryId, cat.getId()));
            item.put("productCount", productCount);

            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public PmsCategory create(Long enterpriseId, CategoryRequest request) {
        // 检查同企业下分类名是否重复
        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<PmsCategory>()
                        .eq(PmsCategory::getEnterpriseId, enterpriseId)
                        .eq(PmsCategory::getName, request.getName()));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "分类名称已存在");
        }

        PmsCategory category = new PmsCategory();
        category.setEnterpriseId(enterpriseId);
        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public PmsCategory update(Long enterpriseId, Long id, CategoryRequest request) {
        PmsCategory category = categoryMapper.selectById(id);
        if (category == null || !category.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 检查同企业下分类名是否重复（排除自身）
        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<PmsCategory>()
                        .eq(PmsCategory::getEnterpriseId, enterpriseId)
                        .eq(PmsCategory::getName, request.getName())
                        .ne(PmsCategory::getId, id));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "分类名称已存在");
        }

        category.setName(request.getName());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    @Transactional
    public void delete(Long enterpriseId, Long id) {
        PmsCategory category = categoryMapper.selectById(id);
        if (category == null || !category.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 检查分类下是否有商品
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<PmsProduct>()
                        .eq(PmsProduct::getEnterpriseId, enterpriseId)
                        .eq(PmsProduct::getCategoryId, id));
        if (productCount > 0) {
            throw new BusinessException(ResultCode.CATEGORY_HAS_PRODUCTS);
        }

        categoryMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchSort(Long enterpriseId, List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());

            PmsCategory category = categoryMapper.selectById(id);
            if (category != null && category.getEnterpriseId().equals(enterpriseId)) {
                category.setSortOrder(sortOrder);
                categoryMapper.updateById(category);
            }
        }
    }
}
