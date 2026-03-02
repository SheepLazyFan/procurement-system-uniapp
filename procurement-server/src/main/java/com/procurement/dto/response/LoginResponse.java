package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应 DTO
 */
@Data
public class LoginResponse implements Serializable {

    /** JWT Token */
    private String token;

    /** 用户信息 */
    private UserInfo user;

    @Data
    public static class UserInfo implements Serializable {
        private Long id;
        private String phone;
        private String role;
        private String nickName;
        private String avatarUrl;
        private Long enterpriseId;
        private String wxOpenid;
    }
}
