package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.BatchImportRequest;
import com.procurement.dto.request.ProductRequest;
import com.procurement.dto.request.StockAdjustRequest;
import com.procurement.dto.response.ImportResultResponse;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商品管理控制器
 */
@Tag(name = "商品管理")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品列表（分页）")
    @GetMapping
    public R<PageResponse<ProductResponse>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean stockWarning) {
        return R.ok(productService.listByPage(loginUser.getEnterpriseId(),
                pageNum, pageSize, categoryId, keyword, stockWarning));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public R<ProductResponse> getById(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long id) {
        return R.ok(productService.getById(loginUser.getEnterpriseId(), id));
    }

    @Operation(summary = "创建商品")
    @PostMapping
    public R<ProductResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody ProductRequest request) {
        return R.ok(productService.create(loginUser.getEnterpriseId(), request));
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public R<ProductResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                      @PathVariable Long id,
                                      @Valid @RequestBody ProductRequest request) {
        return R.ok(productService.update(loginUser.getEnterpriseId(), id, request));
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                           @PathVariable Long id) {
        productService.delete(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "调整库存")
    @PutMapping("/{id}/stock")
    public R<Map<String, Integer>> adjustStock(@AuthenticationPrincipal LoginUser loginUser,
                                                @PathVariable Long id,
                                                @Valid @RequestBody StockAdjustRequest request) {
        Integer newStock = productService.adjustStock(loginUser.getEnterpriseId(), id, request);
        return R.ok(Map.of("stock", newStock));
    }

    @Operation(summary = "库存预警列表")
    @GetMapping("/stock-warnings")
    public R<PageResponse<ProductResponse>> stockWarnings(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(productService.stockWarnings(loginUser.getEnterpriseId(), pageNum, pageSize));
    }

    @Operation(summary = "批量导入商品（暂未开放）")
    @PostMapping("/batch-import")
    public R<ImportResultResponse> batchImport(@AuthenticationPrincipal LoginUser loginUser,
                                                @Valid @RequestBody BatchImportRequest request) {
        return R.ok(productService.batchImport(
                loginUser.getEnterpriseId(), loginUser.getUserId(), request));
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/import-template")
    public R<String> importTemplate() {
        // TODO: 返回 Excel 模板文件流
        return R.ok("导入模板功能开发中");
    }
}
