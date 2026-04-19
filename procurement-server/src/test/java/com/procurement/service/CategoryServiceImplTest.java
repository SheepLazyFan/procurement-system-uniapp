package com.procurement.service;

import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.CategoryRequest;
import com.procurement.entity.PmsCategory;
import com.procurement.mapper.CategoryMapper;
import com.procurement.mapper.ProductMapper;
import com.procurement.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CategoryServiceImpl 单元测试
 * <p>
 * 测试范围：分类创建重名校验、更新企业隔离、删除前置守卫、enterpriseId 空值防御。
 * 依赖全部 Mock，不启动 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl — 商品分类服务")
class CategoryServiceImplTest {

    @Mock private CategoryMapper categoryMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    // ===========================================================
    // 辅助方法
    // ===========================================================

    private PmsCategory buildCategory(Long id, Long enterpriseId, String name) {
        PmsCategory c = new PmsCategory();
        c.setId(id);
        c.setEnterpriseId(enterpriseId);
        c.setName(name);
        c.setSortOrder(0);
        return c;
    }

    private CategoryRequest buildRequest(String name, Integer sortOrder) {
        CategoryRequest req = new CategoryRequest();
        req.setName(name);
        req.setSortOrder(sortOrder);
        return req;
    }

    // ===========================================================
    // 1. create — 创建分类
    // ===========================================================

    @Nested
    @DisplayName("create — 分类创建")
    class CreateTests {

        @Test
        @DisplayName("Should create category with default sortOrder 0 when not provided")
        void should_createWithDefaultSortOrder_when_sortOrderIsNull() {
            // Arrange
            when(categoryMapper.selectCount(any())).thenReturn(0L);

            // Act
            categoryService.create(1L, buildRequest("日用品", null));

            // Assert
            ArgumentCaptor<PmsCategory> captor = ArgumentCaptor.forClass(PmsCategory.class);
            verify(categoryMapper).insert(captor.capture());
            assertThat(captor.getValue().getSortOrder()).isEqualTo(0);
            assertThat(captor.getValue().getName()).isEqualTo("日用品");
            assertThat(captor.getValue().getEnterpriseId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw CONFLICT when category name already exists in same enterprise")
        void should_throwConflict_when_duplicateNameInSameEnterprise() {
            // Arrange — 同企业下已有同名分类
            when(categoryMapper.selectCount(any())).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.create(1L, buildRequest("食品", null)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.CONFLICT.getCode()));
        }
    }

    // ===========================================================
    // 2. update — 更新分类
    // ===========================================================

    @Nested
    @DisplayName("update — 分类更新")
    class UpdateTests {

        @Test
        @DisplayName("Should throw NOT_FOUND when updating another enterprise's category")
        void should_throwNotFound_when_updatingOtherEnterpriseCategory() {
            // Arrange
            PmsCategory category = buildCategory(10L, 999L, "其他企业的分类");
            when(categoryMapper.selectById(10L)).thenReturn(category);

            // Act & Assert — 企业 1 试图更新企业 999 的分类
            assertThatThrownBy(() -> categoryService.update(1L, 10L, buildRequest("新名", null)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("Should throw CONFLICT when renaming to existing name (excluding self)")
        void should_throwConflict_when_renamingToExistingName() {
            // Arrange
            PmsCategory category = buildCategory(20L, 1L, "原名");
            when(categoryMapper.selectById(20L)).thenReturn(category);
            when(categoryMapper.selectCount(any())).thenReturn(1L); // 排除自身后仍有重名

            // Act & Assert
            assertThatThrownBy(() -> categoryService.update(1L, 20L, buildRequest("已存在的名", null)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.CONFLICT.getCode()));
        }
    }

    // ===========================================================
    // 3. delete — 删除分类
    // ===========================================================

    @Nested
    @DisplayName("delete — 分类删除")
    class DeleteTests {

        @Test
        @DisplayName("Should throw CATEGORY_HAS_PRODUCTS when category has associated products")
        void should_throwCategoryHasProducts_when_productsExist() {
            // Arrange
            PmsCategory category = buildCategory(30L, 1L, "有商品的分类");
            when(categoryMapper.selectById(30L)).thenReturn(category);
            when(productMapper.selectCount(any())).thenReturn(5L); // 该分类下有 5 个商品

            // Act & Assert
            assertThatThrownBy(() -> categoryService.delete(1L, 30L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.CATEGORY_HAS_PRODUCTS.getCode()));
        }

        @Test
        @DisplayName("Should delete category when it has no products")
        void should_deleteCategory_when_noProductsExist() {
            // Arrange
            PmsCategory category = buildCategory(31L, 1L, "空分类");
            when(categoryMapper.selectById(31L)).thenReturn(category);
            when(productMapper.selectCount(any())).thenReturn(0L);

            // Act
            categoryService.delete(1L, 31L);

            // Assert
            verify(categoryMapper).deleteById(31L);
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when deleting non-existent category")
        void should_throwNotFound_when_categoryDoesNotExist() {
            // Arrange
            when(categoryMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.delete(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ResultCode.NOT_FOUND.getCode()));
        }
    }

    // ===========================================================
    // 4. enterpriseId 空值防御
    // ===========================================================

    @Test
    @DisplayName("Should throw ENTERPRISE_NOT_FOUND when enterpriseId is null")
    void should_throwEnterpriseNotFound_when_enterpriseIdIsNull() {
        assertThatThrownBy(() -> categoryService.create(null, buildRequest("测试", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.ENTERPRISE_NOT_FOUND.getCode()));
    }
}
