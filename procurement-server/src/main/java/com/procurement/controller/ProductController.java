package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.ProductRequest;
import com.procurement.dto.request.StockAdjustRequest;
import com.procurement.dto.response.ImportResultResponse;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
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

    /** SELLER(memberRole=null) 和 ADMIN 才能看到成本价 */
    private boolean hasFinancialAccess(LoginUser loginUser) {
        String role = loginUser.getMemberRole();
        return role == null || "ADMIN".equals(role);
    }

    @Operation(summary = "商品列表（分页）")
    @GetMapping
    public R<PageResponse<ProductResponse>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean stockWarning,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer maxStock,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sortBy) {
        PageResponse<ProductResponse> result = productService.listByPage(loginUser.getEnterpriseId(),
                pageNum, pageSize, categoryId, keyword, stockWarning, supplierId,
                minPrice, maxPrice, minStock, maxStock, status, sortBy);
        if (!hasFinancialAccess(loginUser)) {
            result.getRecords().forEach(p -> p.setCostPrice(null));
        }
        return R.ok(result);
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public R<ProductResponse> getById(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long id) {
        ProductResponse result = productService.getById(loginUser.getEnterpriseId(), id);
        if (!hasFinancialAccess(loginUser)) {
            result.setCostPrice(null);
        }
        return R.ok(result);
    }

    @Operation(summary = "创建商品")
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public R<ProductResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody ProductRequest request) {
        return R.ok(productService.create(loginUser.getEnterpriseId(), request));
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public R<ProductResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                      @PathVariable Long id,
                                      @Valid @RequestBody ProductRequest request) {
        return R.ok(productService.update(loginUser.getEnterpriseId(), id, request));
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public R<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                           @PathVariable Long id) {
        productService.delete(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "调整库存")
    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'WAREHOUSE')")
    public R<Map<String, Integer>> adjustStock(@AuthenticationPrincipal LoginUser loginUser,
                                                @PathVariable Long id,
                                                @Valid @RequestBody StockAdjustRequest request) {
        Integer newStock = productService.adjustStock(loginUser.getEnterpriseId(), id, request, loginUser.getMemberRole());
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

    @Operation(summary = "批量导入商品")
    @PostMapping("/batch-import")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public R<ImportResultResponse> batchImport(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "duplicateStrategy", defaultValue = "SKIP") String duplicateStrategy,
            @RequestParam(value = "importMode", defaultValue = "TEMPLATE") String importMode) {
        if (file.isEmpty()) {
            return R.fail("请选择要导入的 Excel 文件");
        }
        return R.ok(productService.batchImport(
                loginUser.getEnterpriseId(), loginUser.getUserId(), file, duplicateStrategy, importMode));
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        productService.downloadImportTemplate(response);
    }
}
