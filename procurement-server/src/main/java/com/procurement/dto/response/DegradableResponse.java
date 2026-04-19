package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 支持降级标识的通用响应包装器。
 * <p>
 * 当某个查询模块发生异常时，Service 层使用该包装器返回默认值 + 降级标识，
 * 而不是直接向上抛出异常导致整个请求 500。
 * <p>
 * 使用方式：
 * <pre>
 * DegradableResponse.ok(data)           // 正常
 * DegradableResponse.degraded(data, warnings) // 降级
 * </pre>
 */
@Data
public class DegradableResponse<T> implements Serializable {

    /** 业务数据 */
    private T data;

    /** 是否存在降级 */
    private Boolean degraded;

    /** 降级告警信息列表 */
    private List<String> warnings;

    /** 正常返回 */
    public static <T> DegradableResponse<T> ok(T data) {
        DegradableResponse<T> resp = new DegradableResponse<>();
        resp.setData(data);
        resp.setDegraded(false);
        resp.setWarnings(List.of());
        return resp;
    }

    /** 降级返回 */
    public static <T> DegradableResponse<T> degraded(T data, List<String> warnings) {
        DegradableResponse<T> resp = new DegradableResponse<>();
        resp.setData(data);
        resp.setDegraded(true);
        resp.setWarnings(warnings != null ? warnings : List.of());
        return resp;
    }

    /** 降级返回（单条告警） */
    public static <T> DegradableResponse<T> degraded(T data, String warning) {
        List<String> warnings = new ArrayList<>();
        warnings.add(warning);
        return degraded(data, warnings);
    }
}
