package com.procurement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurement.config.WxConfig;
import com.procurement.service.WxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信服务实现 — 小程序 code2Session
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxServiceImpl implements WxService {

    private final WxConfig wxConfig;
    private final ObjectMapper objectMapper;

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> code2Session(String code) {
        String url = String.format(CODE2SESSION_URL,
                wxConfig.getAppId(), wxConfig.getAppSecret(), code);

        RestTemplate restTemplate = new RestTemplate();
        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("微信 code2Session 响应: {}", response);
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            log.error("微信 code2Session 调用失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("errcode", -1);
            errorResult.put("errmsg", e.getMessage());
            return errorResult;
        }
    }
}
