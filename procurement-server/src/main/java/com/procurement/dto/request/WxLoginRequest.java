package com.procurement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 微信授权登录请求 DTO
 */
@Data
public class WxLoginRequest implements Serializable {

    /** 微信 login code（wx.login 返回） */
    @NotBlank(message = "微信登录 code 不能为空")
    private String code;

    /** 微信昵称 */
    private String nickName;

    /** 头像 URL */
    private String avatarUrl;
}
