package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.CategoryRequest;
import com.procurement.entity.PmsCategory;
import com.procurement.entity.PmsProduct;
import com.procurement.mapper.CategoryMapper;
import com.procurement.mapper.ProductMapper;
import com.procurement.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 商品分类服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    /**
     * 校验企业上下文，避免 enterprise_id 为空导致数据库 500
     */
    private void requireEnterpriseContext(Long enterpriseId) {
        if (enterpriseId == null) {
            throw new BusinessException(ResultCode.ENTERPRISE_NOT_FOUND.getCode(), "请先创建或加入企业");
        }
    }

    @Override
    public List<Map<String, Object>> listByEnterprise(Long enterpriseId) {
        requireEnterpriseContext(enterpriseId);
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
        requireEnterpriseContext(enterpriseId);
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
        requireEnterpriseContext(enterpriseId);
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
        requireEnterpriseContext(enterpriseId);
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
        requireEnterpriseContext(enterpriseId);
        for (Map<String, Object> item : sortList) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());

            // 直接 UPDATE，避免先 SELECT 再 UPDATE 的 2N 查询
            categoryMapper.update(null, new LambdaUpdateWrapper<PmsCategory>()
                    .eq(PmsCategory::getId, id)
                    .eq(PmsCategory::getEnterpriseId, enterpriseId)
                    .set(PmsCategory::getSortOrder, sortOrder));
        }
    }

    @Override
    public Map<Long, Long> getFilteredStats(Long enterpriseId, String keyword, Boolean stockWarning,
                                            BigDecimal minPrice, BigDecimal maxPrice,
                                            Integer minStock, Integer maxStock, Integer status) {
        requireEnterpriseContext(enterpriseId);
        // 一条 GROUP BY SQL 返回全部分类的命中数，避免 N 次 selectCount
        QueryWrapper<PmsProduct> wrapper = new QueryWrapper<PmsProduct>()
                .select("category_id", "COUNT(*) AS cnt")
                .eq("enterprise_id", enterpriseId)
                .groupBy("category_id");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("name", keyword).or().like("spec", keyword));
        }
        if (Boolean.TRUE.equals(stockWarning)) {
            wrapper.apply("stock <= stock_warning AND stock_warning IS NOT NULL");
        }
        if (minPrice != null) wrapper.ge("price", minPrice);
        if (maxPrice != null) wrapper.le("price", maxPrice);
        if (minStock != null) wrapper.ge("stock", minStock);
        if (maxStock != null) wrapper.le("stock", maxStock);
        if (status != null) wrapper.eq("status", status);

        List<Map<String, Object>> rows = productMapper.selectMaps(wrapper);
        Map<Long, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object catId = row.get("category_id");
            Object cnt = row.get("cnt");
            if (catId != null && cnt != null) {
                result.put(((Number) catId).longValue(), ((Number) cnt).longValue());
            }
        }
        return result;
    }
}
