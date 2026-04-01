package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 企业信息响应 DTO
 */
@Data
public class EnterpriseResponse implements Serializable {
    private Long id;
    private String name;
    private String address;
    private String contactPhone;
    private String contactName;
    private String inviteCode;
    private String logoUrl;
    private String paymentQrUrl;
}
