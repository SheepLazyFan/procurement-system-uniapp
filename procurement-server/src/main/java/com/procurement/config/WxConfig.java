package com.procurement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 微信小程序 AppID / Secret 配置
 */
@Data
@Configuration
public class WxConfig {

    @Value("${wx.miniapp.app-id}")
    private String appId;

    @Value("${wx.miniapp.app-secret}")
    private String appSecret;
}
