package com.procurement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 企业创建/更新请求 DTO
 */
@Data
public class EnterpriseRequest implements Serializable {

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 100, message = "企业名称最长100字符")
    private String name;

    @Size(max = 300, message = "地址最长300字符")
    private String address;

    @Size(max = 20, message = "联系电话最长20字符")
    private String contactPhone;

    @Size(max = 50, message = "联系人最长50字符")
    private String contactName;

    @Size(max = 500, message = "收款二维码URL最长500字符")
    private String paymentQrUrl;

    @Size(max = 500, message = "Logo URL最长500字符")
    private String logoUrl;
}
