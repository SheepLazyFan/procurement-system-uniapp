package com.procurement.common.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {}

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 批量导入最大行数 */
    public static final int BATCH_IMPORT_MAX_ROWS = 500;

    /** Redis Key 前缀 — JWT 黑名单 */
    public static final String REDIS_TOKEN_BLACKLIST = "token:blacklist:";
}
