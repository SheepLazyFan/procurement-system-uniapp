package com.procurement.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.excel.ProductImportData;
import com.procurement.dto.excel.SupplierImportData;
import com.procurement.dto.request.BatchImportRequest;
import com.procurement.dto.request.ProductRequest;
import com.procurement.dto.request.StockAdjustRequest;
import com.procurement.dto.response.ImportResultResponse;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductResponse;
import com.procurement.entity.PmsCategory;
import com.procurement.entity.PmsProduct;
import com.procurement.entity.PmsProductSupplier;
import com.procurement.mapper.CategoryMapper;
import com.procurement.mapper.ProductMapper;
import com.procurement.mapper.ProductSupplierMapper;
import com.procurement.service.ProductService;
import com.procurement.service.StockWarningNotificationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductSupplierMapper productSupplierMapper;
    private final StockWarningNotificationService notificationService;

    @Override
    public PageResponse<ProductResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                                     Long categoryId, String keyword, Boolean stockWarning,
                                                     Long supplierId,
                                                     BigDecimal minPrice, BigDecimal maxPrice,
                                                     Integer minStock, Integer maxStock,
                                                     Integer status, String sortBy) {
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
            wrapper.apply("stock <= stock_warning AND stock_warning IS NOT NULL");
        }
        if (supplierId != null) {
            List<Long> productIds = productSupplierMapper.selectList(
                    new LambdaQueryWrapper<PmsProductSupplier>()
                            .eq(PmsProductSupplier::getSupplierId, supplierId)
                            .eq(PmsProductSupplier::getEnterpriseId, enterpriseId)
                            .select(PmsProductSupplier::getProductId)
            ).stream().map(PmsProductSupplier::getProductId).toList();
            if (productIds.isEmpty()) {
                return PageResponse.of(Collections.emptyList(), 0L, pageNum, pageSize);
            }
            wrapper.in(PmsProduct::getId, productIds);
        }
        if (minPrice != null) {
            wrapper.ge(PmsProduct::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(PmsProduct::getPrice, maxPrice);
        }
        if (minStock != null) {
            wrapper.ge(PmsProduct::getStock, minStock);
        }
        if (maxStock != null) {
            wrapper.le(PmsProduct::getStock, maxStock);
        }
        if (status != null) {
            wrapper.eq(PmsProduct::getStatus, status);
        }

        // 排序
        if (StringUtils.hasText(sortBy)) {
            switch (sortBy) {
                case "priceAsc" -> wrapper.orderByAsc(PmsProduct::getPrice);
                case "priceDesc" -> wrapper.orderByDesc(PmsProduct::getPrice);
                case "stockAsc" -> wrapper.orderByAsc(PmsProduct::getStock);
                case "stockDesc" -> wrapper.orderByDesc(PmsProduct::getStock);
                case "newest" -> wrapper.orderByDesc(PmsProduct::getId);
                default -> wrapper.orderByDesc(PmsProduct::getId);
            }
        } else {
            wrapper.orderByDesc(PmsProduct::getId);
        }

        Page<PmsProduct> page = productMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<ProductResponse> records = batchConvert(page.getRecords());

        // 当按供应商过滤时，填充 supplyPrice（快速采购预填价格使用）
        if (supplierId != null && !records.isEmpty()) {
            List<Long> productIds = records.stream().map(ProductResponse::getId).toList();
            Map<Long, BigDecimal> supplyPriceMap = productSupplierMapper.selectList(
                            new LambdaQueryWrapper<PmsProductSupplier>()
                                    .eq(PmsProductSupplier::getSupplierId, supplierId)
                                    .eq(PmsProductSupplier::getEnterpriseId, enterpriseId)
                                    .in(PmsProductSupplier::getProductId, productIds)
                                    .select(PmsProductSupplier::getProductId, PmsProductSupplier::getSupplyPrice))
                    .stream().collect(Collectors.toMap(
                            PmsProductSupplier::getProductId, PmsProductSupplier::getSupplyPrice));
            records.forEach(r -> r.setSupplyPrice(supplyPriceMap.get(r.getId())));
        }

        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
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
        product.setQrcodeImage(request.getQrcodeImage()); // 二维码图片 — TODO: 部署后 URL 迁移至 COS
        product.setDescription(request.getDescription());
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
        product.setQrcodeImage(request.getQrcodeImage()); // 二维码图片 — TODO: 部署后 URL 迁移至 COS
        product.setDescription(request.getDescription());
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
    public Integer adjustStock(Long enterpriseId, Long productId, StockAdjustRequest request, String callerMemberRole) {
        // 权限由 Controller 层 @PreAuthorize("hasAnyRole('SELLER','ADMIN','WAREHOUSE')") 统一拦截
        // SALES 角色在 Controller 层被拦截，不会到达此处
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

        // 出库：事务提交后异步检查库存预警；入库：若恢复预警线以上则清除去重 key
        final Long pid = productId;
        final Long eid = enterpriseId;
        final int preStock = product.getStock(); // 操作前库存（adjustStock 只改 DB，不改 Java 对象）
        if ("OUT".equals(request.getType())) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.checkAndNotify(Map.of(pid, preStock), eid);
                    }
                });
            } else {
                notificationService.checkAndNotify(Map.of(pid, preStock), enterpriseId);
            }
        } else {
            // 入库：清除去重 key，让库存再次跌破时能重新通知
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.clearDedupOnRestock(List.of(pid), eid);
                    }
                });
            } else {
                notificationService.clearDedupOnRestock(List.of(pid), enterpriseId);
            }
        }

        // 查询最新库存
        PmsProduct updated = productMapper.selectById(productId);
        return updated.getStock();
    }

    @Override
    public PageResponse<ProductResponse> stockWarnings(Long enterpriseId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getEnterpriseId, enterpriseId)
                .apply("stock <= stock_warning AND stock_warning > 0")
                .orderByAsc(PmsProduct::getStock);

        Page<PmsProduct> page = productMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        return PageResponse.of(batchConvert(page.getRecords()), page.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional
    public ImportResultResponse batchImport(Long enterpriseId, Long userId,
                                            MultipartFile file, String duplicateStrategy, String importMode) {
        boolean supplierMode = "SUPPLIER".equalsIgnoreCase(importMode);

        // 1. 读取 Excel → 统一为 ProductImportData 列表
        List<ProductImportData> dataList;
        try {
            if (supplierMode) {
                dataList = readSupplierExcel(file);
            } else {
                dataList = EasyExcel.read(file.getInputStream())
                        .head(ProductImportData.class)
                        .sheet()
                        .doReadSync();
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Excel 文件读取失败");
        } catch (Exception e) {
            log.error("Excel 解析异常: ", e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "Excel 解析失败：" + e.getMessage());
        }

        if (dataList == null || dataList.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Excel 中没有数据行");
        }
        if (dataList.size() > 500) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "单次最多导入 500 条，当前 " + dataList.size() + " 条");
        }

        // 2. 预加载本企业所有分类（名称 → ID 映射）
        List<PmsCategory> existingCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<PmsCategory>().eq(PmsCategory::getEnterpriseId, enterpriseId));
        Map<String, Long> categoryNameToId = new HashMap<>();
        existingCategories.forEach(c -> categoryNameToId.put(c.getName(), c.getId()));

        // 3. 预加载本企业所有商品名（用于重复判断）
        List<PmsProduct> existingProducts = productMapper.selectList(
                new LambdaQueryWrapper<PmsProduct>().eq(PmsProduct::getEnterpriseId, enterpriseId));
        Map<String, PmsProduct> productNameMap = new HashMap<>();
        existingProducts.forEach(p -> productNameMap.put(p.getName(), p));

        boolean overwrite = "OVERWRITE".equalsIgnoreCase(duplicateStrategy);
        int successCount = 0;
        int failCount = 0;
        int newCategoryCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        // 数据起始行号（供应商模式第3行起，标准模板第2行起）
        int dataStartRow = supplierMode ? 3 : 2;

        // 4. 逐行校验 & 导入
        for (int i = 0; i < dataList.size(); i++) {
            int rowNum = i + dataStartRow;
            ProductImportData row = dataList.get(i);

            // 4.1 必填校验
            List<String> missing = new ArrayList<>();
            if (!StringUtils.hasText(row.getCategoryName())) missing.add("商品分类");
            if (!StringUtils.hasText(row.getName())) missing.add("商品名称");
            if (!StringUtils.hasText(row.getUnit())) missing.add("计量单位");
            if (row.getPrice() == null || row.getPrice().compareTo(BigDecimal.ZERO) <= 0) missing.add("销售单价(需>0)");

            if (!missing.isEmpty()) {
                failCount++;
                errors.add(Map.of("row", rowNum, "name", row.getName() != null ? row.getName() : "",
                        "reason", "缺少必填字段：" + String.join("、", missing)));
                continue;
            }

            // 4.2 商品名称长度校验
            if (row.getName().length() > 100) {
                failCount++;
                errors.add(Map.of("row", rowNum, "name", row.getName(), "reason", "商品名称超过 100 字"));
                continue;
            }

            // 4.3 分类处理（不存在则自动新建）
            Long categoryId = categoryNameToId.get(row.getCategoryName().trim());
            if (categoryId == null) {
                PmsCategory newCat = new PmsCategory();
                newCat.setEnterpriseId(enterpriseId);
                newCat.setName(row.getCategoryName().trim());
                newCat.setSortOrder(0);
                categoryMapper.insert(newCat);
                categoryId = newCat.getId();
                categoryNameToId.put(newCat.getName(), categoryId);
                newCategoryCount++;
            }

            // 4.4 重复商品处理
            PmsProduct existing = productNameMap.get(row.getName().trim());
            if (existing != null) {
                if (!overwrite) {
                    failCount++;
                    errors.add(Map.of("row", rowNum, "name", row.getName(), "reason", "商品已存在（跳过）"));
                    continue;
                }
                // 覆盖更新
                existing.setCategoryId(categoryId);
                existing.setSpec(row.getSpec());
                existing.setUnit(row.getUnit().trim());
                existing.setPrice(row.getPrice());
                existing.setCostPrice(row.getCostPrice() != null ? row.getCostPrice() : BigDecimal.ZERO);
                if (row.getStock() != null) existing.setStock(row.getStock());
                if (row.getStockWarning() != null) existing.setStockWarning(row.getStockWarning());
                if (row.getQrcodeImageUrl() != null && !row.getQrcodeImageUrl().isBlank()) {
                    existing.setQrcodeImage(row.getQrcodeImageUrl().trim());
                }
                productMapper.updateById(existing);
                successCount++;
            } else {
                // 新增
                PmsProduct product = new PmsProduct();
                product.setEnterpriseId(enterpriseId);
                product.setCategoryId(categoryId);
                product.setName(row.getName().trim());
                product.setSpec(row.getSpec());
                product.setUnit(row.getUnit().trim());
                product.setPrice(row.getPrice());
                product.setCostPrice(row.getCostPrice() != null ? row.getCostPrice() : BigDecimal.ZERO);
                product.setStock(row.getStock() != null ? row.getStock() : 0);
                product.setStockWarning(row.getStockWarning());  // null = 未配置阈值
                if (row.getQrcodeImageUrl() != null && !row.getQrcodeImageUrl().isBlank()) {
                    product.setQrcodeImage(row.getQrcodeImageUrl().trim());
                }
                product.setStatus(1);
                productMapper.insert(product);
                productNameMap.put(product.getName(), product);
                successCount++;
            }
        }

        ImportResultResponse response = new ImportResultResponse();
        response.setTotalCount(dataList.size());
        response.setSuccessCount(successCount);
        response.setFailCount(failCount);
        response.setNewCategoryCount(newCategoryCount);
        response.setErrors(errors);

        log.info("批量导入完成。mode={}, enterpriseId={}, total={}, success={}, fail={}, newCategories={}",
                importMode, enterpriseId, dataList.size(), successCount, failCount, newCategoryCount);
        return response;
    }

    /**
     * 读取供应商价格表 Excel → 转换为标准 ProductImportData 列表
     * 供应商 Excel: 第1行=标题，第2行=表头，第3行起=数据
     * 跳过: A列(序号)、C列(图片)、D列(二维码)、F列(单价)、G列(销售单位)
     * 映射: B→分类, E→名称, H→规格, I→单位, J→进价, K→售价
     */
    private List<ProductImportData> readSupplierExcel(MultipartFile file) throws IOException {
        List<SupplierImportData> rawList = EasyExcel.read(file.getInputStream())
                .head(SupplierImportData.class)
                .sheet()
                .headRowNumber(2)   // 第1行标题 + 第2行表头，数据从第3行起
                .doReadSync();

        if (rawList == null) return Collections.emptyList();

        List<ProductImportData> result = new ArrayList<>();
        for (SupplierImportData raw : rawList) {
            // 过滤空行和非数据行（产品名称为空则跳过）
            if (!StringUtils.hasText(raw.getName())) continue;
            // 过滤备注行（名称含"备注"、"浏阳"等非商品文字）
            String name = raw.getName().trim();
            if (name.startsWith("备注") || name.length() > 100) continue;

            ProductImportData item = new ProductImportData();
            item.setCategoryName(raw.getCategoryName());
            item.setName(name);
            item.setSpec(raw.getSpec());
            item.setUnit(raw.getUnit());
            item.setPrice(raw.getPrice());
            item.setCostPrice(raw.getCostPrice());
            // 供应商表无库存信息，默认不设置（后续走默认 0）
            result.add(item);
        }
        return result;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        // 准备示例数据（2 行样例让用户理解格式）
        List<ProductImportData> sampleData = List.of(
                ProductImportData.builder()
                        .categoryName("饮料")
                        .name("可口可乐 330ml")
                        .spec("330ml×24罐/箱")
                        .unit("箱")
                        .price(new BigDecimal("68.00"))
                        .costPrice(new BigDecimal("52.00"))
                        .stock(100)
                        .stockWarning(20)
                        .qrcodeImageUrl("https://example.com/qrcode/demo.jpg（选填）")
                        .build(),
                ProductImportData.builder()
                        .categoryName("饮料")
                        .name("农夫山泉 550ml")
                        .spec("550ml×24瓶/箱")
                        .unit("箱")
                        .price(new BigDecimal("28.00"))
                        .costPrice(new BigDecimal("18.00"))
                        .stock(200)
                        .stockWarning(30)
                        .build(),
                ProductImportData.builder()
                        .categoryName("零食")
                        .name("乐事薯片原味")
                        .spec("75g/袋")
                        .unit("袋")
                        .price(new BigDecimal("7.50"))
                        .costPrice(new BigDecimal("4.80"))
                        .stock(50)
                        .stockWarning(10)
                        .build()
        );

        String fileName = URLEncoder.encode("商品导入模板", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), ProductImportData.class)
                .sheet("商品导入模板")
                .doWrite(sampleData);
    }

    /**
     * Entity → Response DTO（单条查询版本）
     */
    private ProductResponse toResponse(PmsProduct product) {
        ProductResponse resp = buildBaseResponse(product);
        PmsCategory category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            resp.setCategoryName(category.getName());
        }
        return resp;
    }

    /** 批量转换：预加载所有分类（消除 N+1） */
    private List<ProductResponse> batchConvert(List<PmsProduct> products) {
        if (products.isEmpty()) return Collections.emptyList();

        Set<Long> categoryIds = products.stream()
                .map(PmsProduct::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PmsCategory> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(PmsCategory::getId, Function.identity()));

        return products.stream().map(p -> {
            ProductResponse resp = buildBaseResponse(p);
            PmsCategory cat = categoryMap.get(p.getCategoryId());
            if (cat != null) resp.setCategoryName(cat.getName());
            return resp;
        }).toList();
    }

    private ProductResponse buildBaseResponse(PmsProduct product) {
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
        resp.setQrcodeImage(product.getQrcodeImage()); // 二维码图片
        resp.setDescription(product.getDescription());
        resp.setCategoryId(product.getCategoryId());
        resp.setStatus(product.getStatus());
        return resp;
    }
}
