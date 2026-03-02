package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.CategoryRequest;
import com.procurement.entity.PmsCategory;
import com.procurement.security.LoginUser;
import com.procurement.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品分类控制器
 */
@Tag(name = "商品分类")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类列表")
    @GetMapping
    public R<List<Map<String, Object>>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(categoryService.listByEnterprise(loginUser.getEnterpriseId()));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public R<PmsCategory> create(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody CategoryRequest request) {
        return R.ok(categoryService.create(loginUser.getEnterpriseId(), request));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public R<PmsCategory> update(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @Valid @RequestBody CategoryRequest request) {
        return R.ok(categoryService.update(loginUser.getEnterpriseId(), id, request));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                           @PathVariable Long id) {
        categoryService.delete(loginUser.getEnterpriseId(), id);
        return R.ok();
    }

    @Operation(summary = "批量更新排序")
    @PutMapping("/sort")
    public R<Void> batchSort(@AuthenticationPrincipal LoginUser loginUser,
                              @RequestBody List<Map<String, Object>> sortList) {
        categoryService.batchSort(loginUser.getEnterpriseId(), sortList);
        return R.ok();
    }
}
