package com.procurement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户创建/更新请求 DTO
 */
@Data
public class CustomerRequest implements Serializable {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称最长100字符")
    private String name;

    @Size(max = 20, message = "手机号最长20字符")
    private String phone;

    @Size(max = 300, message = "地址最长300字符")
    private String address;

    @Size(max = 500, message = "备注最长500字符")
    private String remark;
}
