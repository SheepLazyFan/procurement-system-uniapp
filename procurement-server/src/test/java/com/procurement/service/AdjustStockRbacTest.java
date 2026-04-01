package com.procurement.service;

import com.procurement.controller.ProductController;
import com.procurement.entity.SysUser;
import com.procurement.security.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * T-08 ~ T-11：adjustStock RBAC 权限验证（纯单元测试）
 * <p>
 * 验证策略：
 * 1. 通过反射读取 ProductController.adjustStock() 方法上的 @PreAuthorize 注解
 * 2. 解析注解中 hasAnyRole('SELLER','ADMIN','WAREHOUSE') 的允许角色列表
 * 3. 构造各角色的 LoginUser，验证其 getAuthorities() 输出是否匹配
 * <p>
 * 不需要启动 Spring 上下文，不需要 MockMvc，不需要网络请求。
 * </p>
 */
@DisplayName("ProductController.adjustStock — RBAC 权限 (T-08~T-11)")
class AdjustStockRbacTest {

    // ======================== 辅助方法 ========================

    /**
     * 构造指定角色的 LoginUser
     * @param userRole  顶级角色（SELLER / MEMBER / BUYER）
     * @param memberRole 团队成员细分角色（ADMIN / SALES / WAREHOUSE），SELLER/BUYER 为 null
     */
    private LoginUser createLoginUser(String userRole, String memberRole) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setPhone("13800000001");
        user.setRole(userRole);
        user.setEnterpriseId(1L);
        user.setIsDeleted(0);
        return new LoginUser(user, memberRole);
    }

    /**
     * 从 @PreAuthorize 注解中提取被允许的角色列表
     * 例如 hasAnyRole('SELLER','ADMIN','WAREHOUSE') → {"ROLE_SELLER","ROLE_ADMIN","ROLE_WAREHOUSE"}
     */
    private Set<String> extractAllowedRoles(String annotationValue) {
        // 匹配 hasAnyRole 或 hasRole 中所有单引号包裹的角色名
        Pattern p = Pattern.compile("'([A-Z_]+)'");
        Matcher m = p.matcher(annotationValue);
        Set<String> roles = new java.util.HashSet<>();
        while (m.find()) {
            roles.add("ROLE_" + m.group(1)); // Spring Security 自动加 ROLE_ 前缀
        }
        return roles;
    }

    /**
     * 获取用户的 authority 字符串集合
     */
    private Set<String> getAuthorityStrings(LoginUser user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // ======================== 测试数据源 ========================

    /**
     * T-08: SELLER    → hasRole('SELLER') → ✅ 允许
     * T-09: ADMIN     → hasRole('ADMIN')  → ✅ 允许
     * T-10: WAREHOUSE → hasRole('WAREHOUSE') → ✅ 允许
     * T-11: SALES     → 无匹配角色 → ❌ 拒绝 (403)
     */
    static Stream<Arguments> roleAccessProvider() {
        return Stream.of(
                //    userRole,  memberRole,   shouldBeAllowed
                Arguments.of("SELLER",  null,        true),   // T-08
                Arguments.of("MEMBER",  "ADMIN",     true),   // T-09
                Arguments.of("MEMBER",  "WAREHOUSE", true),   // T-10
                Arguments.of("MEMBER",  "SALES",     false)   // T-11
        );
    }

    // ======================== 测试用例 ========================

    @Test
    @DisplayName("adjustStock 方法必须有 @PreAuthorize 注解")
    void adjustStock_should_have_PreAuthorize_annotation() throws NoSuchMethodException {
        // 找到 adjustStock 方法（可能有多个同名重载，按参数匹配）
        Method method = null;
        for (Method m : ProductController.class.getDeclaredMethods()) {
            if ("adjustStock".equals(m.getName())) {
                method = m;
                break;
            }
        }
        assertThat(method)
                .as("ProductController 必须包含 adjustStock 方法")
                .isNotNull();

        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("adjustStock 必须有 @PreAuthorize 注解")
                .isNotNull();

        String value = annotation.value();
        assertThat(value)
                .as("@PreAuthorize 注解值必须包含 hasAnyRole")
                .contains("hasAnyRole");

        // 确认只允许 SELLER, ADMIN, WAREHOUSE
        Set<String> allowedRoles = extractAllowedRoles(value);
        assertThat(allowedRoles)
                .as("adjustStock 应仅允许 SELLER, ADMIN, WAREHOUSE")
                .containsExactlyInAnyOrder("ROLE_SELLER", "ROLE_ADMIN", "ROLE_WAREHOUSE");
    }

    @ParameterizedTest(name = "T-{index}+07: {0}(memberRole={1}) → allowed={2}")
    @MethodSource("roleAccessProvider")
    @DisplayName("adjustStock — RBAC 角色权限匹配")
    void should_matchRbacPolicy_when_roleVaries(
            String userRole, String memberRole, boolean shouldBeAllowed) throws Exception {

        // Arrange — 获取注解中的允许角色
        Method method = null;
        for (Method m : ProductController.class.getDeclaredMethods()) {
            if ("adjustStock".equals(m.getName())) {
                method = m;
                break;
            }
        }
        assertThat(method).isNotNull();
        Set<String> allowedRoles = extractAllowedRoles(method.getAnnotation(PreAuthorize.class).value());

        // Act — 构造该角色的 LoginUser 并获取其权限集
        LoginUser loginUser = createLoginUser(userRole, memberRole);
        Set<String> userAuthorities = getAuthorityStrings(loginUser);

        // Assert — 判断用户权限是否与允许角色有交集
        boolean hasPermission = userAuthorities.stream().anyMatch(allowedRoles::contains);

        String roleLabel = memberRole != null ? memberRole : userRole;
        if (shouldBeAllowed) {
            assertThat(hasPermission)
                    .as("%s 角色 (authorities=%s) 应有权调用 adjustStock (allowedRoles=%s)",
                            roleLabel, userAuthorities, allowedRoles)
                    .isTrue();
        } else {
            assertThat(hasPermission)
                    .as("%s 角色 (authorities=%s) 不应有权调用 adjustStock (allowedRoles=%s)",
                            roleLabel, userAuthorities, allowedRoles)
                    .isFalse();
        }
    }
}
