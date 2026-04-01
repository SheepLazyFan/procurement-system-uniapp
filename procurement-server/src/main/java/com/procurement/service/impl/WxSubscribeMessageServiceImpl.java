package com.procurement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 微信订阅消息推送服务
 * <p>
 * 使用「商品调仓通知」模板（编号 26427）推送库存预警：
 * - thing1: 商品名称
 * - number2: 调整前库存（操作前的库存数量）
 * - number3: 调整后库存（操作后的库存数量）
 * - time4: 调整时间（→ 提醒时间）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxSubscribeMessageServiceImpl {

    private final WxAccessTokenServiceImpl wxAccessTokenService;

    @Value("${wx.subscribe.stock-warning-template-id:}")
    private String stockWarningTemplateId;

    /**
     * 数据备份失败告警模板 ID（可选）。
     * 留空（默认）时告警消息仅记录日志，不推送；
     * 配置后将发送订阅消息，模板需包含 thing1（内容）和 time4（时间）两个字段。
     */
    @Value("${wx.subscribe.backup-alert-template-id:}")
    private String backupAlertTemplateId;

    private static final String SEND_URL =
            "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=%s";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 发送库存预警通知
     *
     * @param openId      用户微信 openId
     * @param productName 商品名称
     * @param beforeStock  操作前库存
     * @param afterStock   操作后库存
     */
    public boolean sendStockWarning(String openId, String productName, int beforeStock, int afterStock) {
        if (stockWarningTemplateId == null || stockWarningTemplateId.isEmpty()) {
            log.warn("stockWarningTemplateId 未配置，跳过推送");
            return false;
        }

        String accessToken = wxAccessTokenService.getAccessToken();
        if (accessToken == null) {
            log.error("获取 access_token 失败，无法推送订阅消息");
            return false;
        }

        try {
            // 构建 JSON 字符串直接发送，避免 HttpEntity<Map> 嵌套序列化兼容性问题
            String jsonBody = String.format(
                    "{\"touser\":\"%s\",\"template_id\":\"%s\",\"page\":\"%s\",\"miniprogram_state\":\"developer\",\"data\":{\"thing1\":{\"value\":\"%s\"},\"number2\":{\"value\":\"%d\"},\"number3\":{\"value\":\"%d\"},\"time4\":{\"value\":\"%s\"}}}",
                    openId,
                    stockWarningTemplateId,
                    "pages/inventory/index",
                    truncate(productName, 20).replace("\"", "\\\""),
                    beforeStock,
                    afterStock,
                    LocalDateTime.now().format(TIME_FMT)
            );

            String url = String.format(SEND_URL, accessToken);
            RestTemplate restTemplate = new RestTemplate();
            log.info("推送订阅消息: openId={}, product={}, body={}", openId, productName, jsonBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(url, entity, Map.class);

            if (result != null && Integer.valueOf(0).equals(result.get("errcode"))) {
                log.info("订阅消息推送成功: openId={}", openId);
                return true;
            } else {
                log.warn("订阅消息推送失败: errcode={}, errmsg={}", result != null ? result.get("errcode") : "null", result != null ? result.get("errmsg") : "null");
                return false;
            }
        } catch (Exception e) {
            log.error("推送订阅消息异常", e);
            return false;
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }

    /**
     * 发送数据备份失败告警通知。
     * <p>
     * 需在配置中设置 {@code wx.subscribe.backup-alert-template-id}；
     * 未配置时仅记录 ERROR 日志，优雅降级。
     * 模板须含 thing1（告警内容）和 time4（发生时间）两个字段。
     * </p>
     *
     * @param openId         企业主 openId
     * @param enterpriseName 企业名称
     * @param failTime       备份失败时间
     */
    public void sendBackupAlert(String openId, String enterpriseName, java.time.LocalDateTime failTime) {
        if (backupAlertTemplateId == null || backupAlertTemplateId.isBlank()) {
            log.error("【备份告警】企业 [{}] 自动备份失败，备份告警推送未配置（wx.subscribe.backup-alert-template-id 为空）",
                    enterpriseName);
            return;
        }

        String accessToken = wxAccessTokenService.getAccessToken();
        if (accessToken == null) {
            log.error("【备份告警】获取 access_token 失败，无法推送备份失败通知");
            return;
        }

        try {
            String content = truncate("企业数据备份失败：" + enterpriseName, 20).replace("\"", "\\\"");
            String jsonBody = String.format(
                    "{\"touser\":\"%s\",\"template_id\":\"%s\",\"page\":\"%s\",\"miniprogram_state\":\"developer\",\"data\":{\"thing1\":{\"value\":\"%s\"},\"time4\":{\"value\":\"%s\"}}}",
                    openId, backupAlertTemplateId, "pages/profile/backup",
                    content, failTime.format(TIME_FMT)
            );

            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(SEND_URL, accessToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(url, entity, Map.class);

            if (result != null && Integer.valueOf(0).equals(result.get("errcode"))) {
                log.info("【备份告警】推送成功: openId={}, enterprise={}", openId, enterpriseName);
            } else {
                log.warn("【备份告警】推送失败: errcode={}, errmsg={}", result != null ? result.get("errcode") : "null", result != null ? result.get("errmsg") : "null");
            }
        } catch (Exception e) {
            log.error("【备份告警】推送订阅消息异常: {}", e.getMessage());
        }
    }
}
