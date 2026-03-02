package com.procurement.service;

/**
 * 短信服务接口 — 腾讯云短信（初期控制台打印模式）
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     */
    void sendSmsCode(String phone, String code);
}
