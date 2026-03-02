package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.BatchImportRequest;
import com.procurement.dto.request.ProductRequest;
import com.procurement.dto.request.StockAdjustRequest;
import com.procurement.dto.response.ImportResultResponse;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductResponse;
import com.procurement.entity.PmsCategory;
import com.procurement.entity.PmsProduct;
import com.procurement.mapper.CategoryMapper;
import com.procurement.mapper.ProductMapper;
import com.procurement.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 商品管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public PageResponse<ProductResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                                     Long categoryId, String keyword, Boolean stockWarning) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getEnterpriseId, enterpriseId);

        if (categoryId != null) {
            wrapper.eq(PmsProduct::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PmsProduct::getName, keyword)
                    .or().like(PmsProduct::getSpec, keyword));
        }
        if (Boolean.TRUE.equals(stockWarning)) {
            // stock < stockWarning AND stockWarning > 0
            wrapper.apply("stock < stock_warning AND stock_warning > 0");
        }

        wrapper.orderByDesc(PmsProduct::getId);

        Page<PmsProduct> page = productMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        return PageResponse.of(page.getRecords().stream()
                .map(this::toResponse).toList(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public ProductResponse getById(Long enterpriseId, Long id) {
        PmsProduct product = productMapper.selectById(id);
        if (product == null || !product.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse create(Long enterpriseId, ProductRequest request) {
        PmsProduct product = new PmsProduct();
        product.setEnterpriseId(enterpriseId);
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setSpec(request.getSpec());
        product.setUnit(request.getUnit());
        product.setPrice(request.getPrice());
        product.setCostPrice(request.getCostPrice());
        product.setStock(request.getStock() != null ? request.getStock() : 0);
        product.setStockWarning(request.getStockWarning() != null ? request.getStockWarning() : 0);
        product.setImages(request.getImages());
        product.setStatus(1);
        productMapper.insert(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long enterpriseId, Long id, ProductRequest request) {
        PmsProduct product = productMapper.selectById(id);
        if (product == null || !product.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setSpec(request.getSpec());
        product.setUnit(request.getUnit());
        product.setPrice(request.getPrice());
        product.setCostPrice(request.getCostPrice());
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getStockWarning() != null) {
            product.setStockWarning(request.getStockWarning());
        }
        product.setImages(request.getImages());
        productMapper.updateById(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public void delete(Long enterpriseId, Long id) {
        PmsProduct product = productMapper.selectById(id);
        if (product == null || !product.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        productMapper.deleteById(id);
    }

    @Override
    @Transactional
    public Integer adjustStock(Long enterpriseId, Long productId, StockAdjustRequest request) {
        PmsProduct product = productMapper.selectById(productId);
        if (product == null || !product.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        int quantity = request.getQuantity();
        if ("OUT".equals(request.getType())) {
            quantity = -quantity;
        }

        int rows = productMapper.adjustStock(productId, quantity);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }

        // 查询最新库存
        PmsProduct updated = productMapper.selectById(productId);
        return updated.getStock();
    }

    @Override
    public PageResponse<ProductResponse> stockWarnings(Long enterpriseId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getEnterpriseId, enterpriseId)
                .apply("stock < stock_warning AND stock_warning > 0")
                .orderByAsc(PmsProduct::getStock);

        Page<PmsProduct> page = productMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        return PageResponse.of(page.getRecords().stream()
                .map(this::toResponse).toList(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public ImportResultResponse batchImport(Long enterpriseId, Long userId, BatchImportRequest request) {
        // 暂不实现业务逻辑，等待与客户沟通具体的表格格式
        ImportResultResponse response = new ImportResultResponse();
        response.setTotalCount(0);
        response.setSuccessCount(0);
        response.setFailCount(0);
        response.setNewCategoryCount(0);
        response.setErrors(java.util.Collections.emptyList());
        log.info("批量导入功能暂未实现，等待与客户沟通表格格式。enterpriseId={}, userId={}", enterpriseId, userId);
        return response;
    }

    /**
     * Entity → Response DTO
     */
    private ProductResponse toResponse(PmsProduct product) {
        ProductResponse resp = new ProductResponse();
        resp.setId(product.getId());
        resp.setName(product.getName());
        resp.setSpec(product.getSpec());
        resp.setUnit(product.getUnit());
        resp.setPrice(product.getPrice());
        resp.setCostPrice(product.getCostPrice());
        resp.setStock(product.getStock());
        resp.setStockWarning(product.getStockWarning());
        resp.setImages(product.getImages());
        resp.setCategoryId(product.getCategoryId());
        resp.setStatus(product.getStatus());

        // 查询分类名称
        PmsCategory category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            resp.setCategoryName(category.getName());
        }
        return resp;
    }
}
