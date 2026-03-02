package com.procurement.common.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {}

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 短信验证码过期时间（秒） */
    public static final int SMS_CODE_EXPIRE_SECONDS = 300;

    /** 短信验证码长度 */
    public static final int SMS_CODE_LENGTH = 6;

    /** 批量导入最大行数 */
    public static final int BATCH_IMPORT_MAX_ROWS = 500;

    /** Redis Key 前缀 — 短信验证码 */
    public static final String REDIS_SMS_PREFIX = "sms:code:";

    /** Redis Key 前缀 — JWT 黑名单 */
    public static final String REDIS_TOKEN_BLACKLIST = "token:blacklist:";
}
