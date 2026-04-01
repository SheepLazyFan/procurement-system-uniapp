package com.procurement.common.result;

/**
 * 统一响应码枚举
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // 参数校验 4xxxx
    PARAM_ERROR(40001, "参数校验失败"),
    PARAM_MISSING(40002, "缺少必要参数"),

    // 认证鉴权 401xx
    UNAUTHORIZED(40100, "未登录或 Token 已过期"),
    TOKEN_INVALID(40101, "Token 无效"),
    TOKEN_EXPIRED(40102, "Token 已过期"),

    // 权限 403xx
    FORBIDDEN(40300, "无权限访问"),

    // 资源 404xx
    NOT_FOUND(40400, "资源不存在"),
    USER_NOT_FOUND(40401, "用户不存在"),
    ENTERPRISE_NOT_FOUND(40402, "企业不存在"),

    // 业务冲突 409xx
    CONFLICT(40900, "业务冲突"),
    ENTERPRISE_ALREADY_EXISTS(40901, "已创建企业，不可重复创建"),
    STOCK_INSUFFICIENT(40902, "库存不足"),
    CATEGORY_HAS_PRODUCTS(40903, "该分类下存在商品，无法删除"),
    WX_LOGIN_FAILED(40904, "微信登录失败"),
    ORDER_STATUS_ERROR(40907, "订单状态不允许此操作"),

    // 服务器 500xx
    INTERNAL_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
