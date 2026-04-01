package com.procurement.common.constant;

/**
 * 用户角色常量
 */
public final class UserConstants {

    private UserConstants() {}

    /** 商家（企业主） */
    public static final String ROLE_SELLER = "SELLER";

    /** 团队成员 */
    public static final String ROLE_MEMBER = "MEMBER";

    /** 买家 */
    public static final String ROLE_BUYER = "BUYER";

    // ========== 团队成员角色（细分） ==========

    /** 管理员 — 等同店主，全部功能 */
    public static final String MEMBER_ROLE_ADMIN = "ADMIN";

    /** 销售员 — 销售订单 + 客户管理 + 查看库存 */
    public static final String MEMBER_ROLE_SALES = "SALES";

    /** 仓管员 — 库存管理 + 采购订单 + 供应商管理 */
    public static final String MEMBER_ROLE_WAREHOUSE = "WAREHOUSE";

    /** 允许的团队成员角色值 */
    public static final java.util.Set<String> VALID_MEMBER_ROLES = java.util.Set.of(
            MEMBER_ROLE_ADMIN, MEMBER_ROLE_SALES, MEMBER_ROLE_WAREHOUSE
    );
}
