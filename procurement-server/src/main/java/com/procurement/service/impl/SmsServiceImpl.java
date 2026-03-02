package com.procurement.service.impl;

import com.procurement.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信服务实现 — 初期使用控制台打印模式，后期接入腾讯云 SMS
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Override
    public void sendSmsCode(String phone, String code) {
        // TODO: 后期对接腾讯云 SMS
        // Credential cred = new Credential(secretId, secretKey);
        // SmsClient client = new SmsClient(cred, "ap-guangzhou");
        // SendSmsRequest req = new SendSmsRequest();
        // ...

        // 当前开发阶段：直接控制台打印验证码
        log.info("============================================");
        log.info("📱 短信验证码 → 手机号: {}, 验证码: {}", phone, code);
        log.info("============================================");
    }
}
