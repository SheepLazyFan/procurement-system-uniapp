package com.procurement.service;

import com.procurement.dto.request.BatchImportRequest;
import com.procurement.dto.request.ProductRequest;
import com.procurement.dto.request.StockAdjustRequest;
import com.procurement.dto.response.ImportResultResponse;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductResponse;

/**
 * 商品管理服务接口
 */
public interface ProductService {

    /**
     * 分页查询商品
     */
    PageResponse<ProductResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                              Long categoryId, String keyword, Boolean stockWarning);

    /**
     * 获取商品详情
     */
    ProductResponse getById(Long enterpriseId, Long id);

    /**
     * 创建商品
     */
    ProductResponse create(Long enterpriseId, ProductRequest request);

    /**
     * 更新商品
     */
    ProductResponse update(Long enterpriseId, Long id, ProductRequest request);

    /**
     * 删除商品
     */
    void delete(Long enterpriseId, Long id);

    /**
     * 调整库存
     */
    Integer adjustStock(Long enterpriseId, Long productId, StockAdjustRequest request);

    /**
     * 库存预警列表
     */
    PageResponse<ProductResponse> stockWarnings(Long enterpriseId, Integer pageNum, Integer pageSize);

    /**
     * 批量导入商品（暂不实现业务逻辑，等待与客户沟通表格格式）
     */
    ImportResultResponse batchImport(Long enterpriseId, Long userId, BatchImportRequest request);
}
