package com.procurement.service;

import java.util.Map;

/**
 * 微信服务接口 — 小程序登录凭证校验
 */
public interface WxService {

    /**
     * 调用微信 code2Session 接口
     *
     * @param code 小程序 wx.login() 获取的 code
     * @return 包含 openid、session_key 等字段的 Map
     */
    Map<String, Object> code2Session(String code);
}
