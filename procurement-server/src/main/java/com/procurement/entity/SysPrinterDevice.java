package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 蓝牙打印机表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_printer_device")
public class SysPrinterDevice extends BaseEntity {

    /** 所属企业 */
    private Long enterpriseId;

    /** 绑定用户 */
    private Long userId;

    /** BLE 设备 ID */
    private String deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 纸张宽度：58/80（mm） */
    private Integer paperWidth;

    /** 是否默认打印机 */
    private Integer isDefault;
}
