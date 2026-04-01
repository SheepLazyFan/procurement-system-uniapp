package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.entity.*;
import com.procurement.mapper.*;
import com.procurement.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.procurement.config.CosConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据备份服务实现
 *
 * 备份范围（按 enterprise_id 隔离）：
 *   - pms_category, pms_product
 *   - crm_customer, crm_supplier
 *   - oms_sales_order, oms_sales_order_item
 *   - oms_purchase_order, oms_purchase_order_item
 *   - sys_team_member
 *
 * 备份格式：JSON
 * 存储位置：本地 backup 目录（后续部署云端迁移至 COS）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final BackupMapper backupMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final SupplierMapper supplierMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final JdbcTemplate jdbcTemplate;
    private final COSClient cosClient;
    private final CosConfig cosConfig;

    @Value("${backup.dir:backup}")
    private String backupDir;

    private final ObjectMapper objectMapper = createObjectMapper();

    /** COS 备份文件路径前缀 */
    private static final String COS_BACKUP_PREFIX = "backup/";

    /** 是否使用本地存储（COS 凭证为占位符时自动降级） */
    private boolean useLocalStorage = false;

    /** 表名映射：JSON key → 数据库表名 */
    private static final Map<String, String> TABLE_NAMES = Map.of(
            "categories", "pms_category",
            "products", "pms_product",
            "customers", "crm_customer",
            "suppliers", "crm_supplier",
            "salesOrders", "oms_sales_order",
            "salesOrderItems", "oms_sales_order_item",
            "purchaseOrders", "oms_purchase_order",
            "purchaseOrderItems", "oms_purchase_order_item",
            "teamMembers", "sys_team_member"
    );

    private static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.enable(SerializationFeature.INDENT_OUTPUT);
        return om;
    }

    @PostConstruct
    public void init() {
        String secretId = cosConfig.getSecretId();
        if (secretId == null || secretId.isBlank() || secretId.startsWith("your-")) {
            useLocalStorage = true;
            log.warn("⚠ COS 凭证未配置，备份降级为本地存储: {}", backupDir);
        } else {
            log.info("✅ 备份使用腾讯云 COS 存储");
        }
    }

    @Override
    @Transactional
    public SysBackup create(Long enterpriseId, String backupType) {
        // 1. 创建备份记录
        SysBackup backup = new SysBackup();
        backup.setEnterpriseId(enterpriseId);
        backup.setBackupType(backupType != null ? backupType : "FULL");
        backup.setStatus("PROCESSING");
        backup.setFileUrl("");
        backup.setFileSize(0L);
        backup.setRemark("数据备份");
        backupMapper.insert(backup);

        try {
            // 2. 查询当前企业的所有业务数据
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("backupId", backup.getId());
            data.put("enterpriseId", enterpriseId);
            data.put("backupTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            data.put("version", "1.0");

            // 商品 & 分类
            data.put("categories", categoryMapper.selectList(
                    new LambdaQueryWrapper<PmsCategory>().eq(PmsCategory::getEnterpriseId, enterpriseId)));
            data.put("products", productMapper.selectList(
                    new LambdaQueryWrapper<PmsProduct>().eq(PmsProduct::getEnterpriseId, enterpriseId)));

            // 客户 & 供应商
            data.put("customers", customerMapper.selectList(
                    new LambdaQueryWrapper<CrmCustomer>().eq(CrmCustomer::getEnterpriseId, enterpriseId)));
            data.put("suppliers", supplierMapper.selectList(
                    new LambdaQueryWrapper<CrmSupplier>().eq(CrmSupplier::getEnterpriseId, enterpriseId)));

            // 销售订单 & 明细
            List<OmsSalesOrder> salesOrders = salesOrderMapper.selectList(
                    new LambdaQueryWrapper<OmsSalesOrder>().eq(OmsSalesOrder::getEnterpriseId, enterpriseId));
            data.put("salesOrders", salesOrders);
            if (!salesOrders.isEmpty()) {
                List<Long> salesOrderIds = salesOrders.stream().map(OmsSalesOrder::getId).toList();
                data.put("salesOrderItems", salesOrderItemMapper.selectList(
                        new LambdaQueryWrapper<OmsSalesOrderItem>().in(OmsSalesOrderItem::getOrderId, salesOrderIds)));
            } else {
                data.put("salesOrderItems", List.of());
            }

            // 采购订单 & 明细
            List<OmsPurchaseOrder> purchaseOrders = purchaseOrderMapper.selectList(
                    new LambdaQueryWrapper<OmsPurchaseOrder>().eq(OmsPurchaseOrder::getEnterpriseId, enterpriseId));
            data.put("purchaseOrders", purchaseOrders);
            if (!purchaseOrders.isEmpty()) {
                List<Long> purchaseOrderIds = purchaseOrders.stream().map(OmsPurchaseOrder::getId).toList();
                data.put("purchaseOrderItems", purchaseOrderItemMapper.selectList(
                        new LambdaQueryWrapper<OmsPurchaseOrderItem>().in(OmsPurchaseOrderItem::getOrderId, purchaseOrderIds)));
            } else {
                data.put("purchaseOrderItems", List.of());
            }

            // 团队成员
            data.put("teamMembers", teamMemberMapper.selectList(
                    new LambdaQueryWrapper<SysTeamMember>().eq(SysTeamMember::getEnterpriseId, enterpriseId)));

            // 统计条目数
            int totalRecords = 0;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof List<?> list) {
                    totalRecords += list.size();
                }
            }

            // 3. 写入备份文件
            String fileName = String.format("backup_%d_%s.json",
                    enterpriseId,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            long fileSize;

            if (useLocalStorage) {
                File dir = new File(backupDir);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IOException("无法创建备份目录: " + dir.getAbsolutePath());
                }
                File file = new File(dir, fileName);
                objectMapper.writeValue(file, data);
                fileSize = file.length();
            } else {
                byte[] jsonBytes = objectMapper.writeValueAsBytes(data);
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(jsonBytes.length);
                metadata.setContentType("application/json; charset=utf-8");
                cosClient.putObject(new PutObjectRequest(cosConfig.getBucket(),
                        COS_BACKUP_PREFIX + fileName,
                        new ByteArrayInputStream(jsonBytes), metadata));
                fileSize = jsonBytes.length;
                log.info("备份文件上传COS: {}", COS_BACKUP_PREFIX + fileName);
            }

            // 4. 更新备份记录
            backup.setFileUrl(fileName);
            backup.setFileSize(fileSize);
            backup.setStatus("COMPLETED");
            backup.setRemark("全量备份，共 " + totalRecords + " 条记录");
            backupMapper.updateById(backup);

            log.info("备份创建成功 enterpriseId={}, file={}, size={}, records={}",
                    enterpriseId, fileName, fileSize, totalRecords);

        } catch (Exception e) {
            log.error("备份失败 enterpriseId={}", enterpriseId, e);
            backup.setStatus("FAILED");
            backup.setRemark("备份失败: " + e.getMessage());
            backupMapper.updateById(backup);
            throw new BusinessException(ResultCode.FAIL.getCode(), "备份失败: " + e.getMessage());
        }

        return backup;
    }

    @Override
    public List<SysBackup> list(Long enterpriseId) {
        return backupMapper.selectList(
                new LambdaQueryWrapper<SysBackup>()
                        .eq(SysBackup::getEnterpriseId, enterpriseId)
                        .orderByDesc(SysBackup::getId));
    }

    @Override
    @Transactional
    public void restore(Long enterpriseId, Long backupId) {
        // 1. 查询备份记录
        SysBackup backup = backupMapper.selectById(backupId);
        if (backup == null || !backup.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!"COMPLETED".equals(backup.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "备份状态异常，无法恢复");
        }

        // 2. 读取备份文件
        try {
            Map<String, Object> data;
            if (useLocalStorage) {
                File file = new File(backupDir, backup.getFileUrl());
                if (!file.exists()) {
                    throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "备份文件不存在，可能已被删除");
                }
                data = objectMapper.readValue(file, new TypeReference<>() {});
            } else {
                try (InputStream is = cosClient.getObject(cosConfig.getBucket(),
                        COS_BACKUP_PREFIX + backup.getFileUrl()).getObjectContent()) {
                    data = objectMapper.readValue(is, new TypeReference<>() {});
                }
            }

            // 3. 清除当前企业的全部业务数据（先删子表，再删主表）
            // 3a. 销售订单明细（通过订单 ID 关联）
            List<OmsSalesOrder> existingSalesOrders = salesOrderMapper.selectList(
                    new LambdaQueryWrapper<OmsSalesOrder>().eq(OmsSalesOrder::getEnterpriseId, enterpriseId));
            if (!existingSalesOrders.isEmpty()) {
                List<Long> orderIds = existingSalesOrders.stream().map(OmsSalesOrder::getId).toList();
                salesOrderItemMapper.delete(new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .in(OmsSalesOrderItem::getOrderId, orderIds));
            }
            salesOrderMapper.delete(new LambdaQueryWrapper<OmsSalesOrder>()
                    .eq(OmsSalesOrder::getEnterpriseId, enterpriseId));

            // 3b. 采购订单明细
            List<OmsPurchaseOrder> existingPurchaseOrders = purchaseOrderMapper.selectList(
                    new LambdaQueryWrapper<OmsPurchaseOrder>().eq(OmsPurchaseOrder::getEnterpriseId, enterpriseId));
            if (!existingPurchaseOrders.isEmpty()) {
                List<Long> orderIds = existingPurchaseOrders.stream().map(OmsPurchaseOrder::getId).toList();
                purchaseOrderItemMapper.delete(new LambdaQueryWrapper<OmsPurchaseOrderItem>()
                        .in(OmsPurchaseOrderItem::getOrderId, orderIds));
            }
            purchaseOrderMapper.delete(new LambdaQueryWrapper<OmsPurchaseOrder>()
                    .eq(OmsPurchaseOrder::getEnterpriseId, enterpriseId));

            // 3c. 商品、分类、客户、供应商、团队成员
            productMapper.delete(new LambdaQueryWrapper<PmsProduct>()
                    .eq(PmsProduct::getEnterpriseId, enterpriseId));
            categoryMapper.delete(new LambdaQueryWrapper<PmsCategory>()
                    .eq(PmsCategory::getEnterpriseId, enterpriseId));
            customerMapper.delete(new LambdaQueryWrapper<CrmCustomer>()
                    .eq(CrmCustomer::getEnterpriseId, enterpriseId));
            supplierMapper.delete(new LambdaQueryWrapper<CrmSupplier>()
                    .eq(CrmSupplier::getEnterpriseId, enterpriseId));
            teamMemberMapper.delete(new LambdaQueryWrapper<SysTeamMember>()
                    .eq(SysTeamMember::getEnterpriseId, enterpriseId));

            // 4. 恢复数据（保留原始 ID，保持外键关联）
            insertListWithId(data, "categories");
            insertListWithId(data, "products");
            insertListWithId(data, "customers");
            insertListWithId(data, "suppliers");
            insertListWithId(data, "salesOrders");
            insertListWithId(data, "salesOrderItems");
            insertListWithId(data, "purchaseOrders");
            insertListWithId(data, "purchaseOrderItems");
            insertListWithId(data, "teamMembers");

            log.info("数据恢复成功 enterpriseId={}, backupId={}", enterpriseId, backupId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("数据恢复失败 enterpriseId={}, backupId={}", enterpriseId, backupId, e);
            throw new BusinessException(ResultCode.FAIL.getCode(), "数据恢复失败: " + e.getMessage());
        }
    }

    /**
     * 获取备份文件（COS 模式下载到临时文件）
     */
    @Override
    public File getBackupFile(Long enterpriseId, Long backupId) {
        SysBackup backup = backupMapper.selectById(backupId);
        if (backup == null || !backup.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!"COMPLETED".equals(backup.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "备份状态异常");
        }
        if (useLocalStorage) {
            File file = new File(backupDir, backup.getFileUrl());
            if (!file.exists()) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "备份文件不存在");
            }
            return file;
        } else {
            try {
                File tempFile = File.createTempFile("backup_download_", ".json");
                tempFile.deleteOnExit();
                cosClient.getObject(new GetObjectRequest(cosConfig.getBucket(),
                        COS_BACKUP_PREFIX + backup.getFileUrl()), tempFile);
                return tempFile;
            } catch (Exception e) {
                log.error("从COS下载备份文件失败: {}", backup.getFileUrl(), e);
                throw new BusinessException(ResultCode.FAIL.getCode(), "备份文件下载失败");
            }
        }
    }

    /**
     * 删除备份（记录 + 文件）
     */
    @Override
    @Transactional
    public void delete(Long enterpriseId, Long backupId) {
        SysBackup backup = backupMapper.selectById(backupId);
        if (backup == null || !backup.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (backup.getFileUrl() != null) {
            if (useLocalStorage) {
                File file = new File(backupDir, backup.getFileUrl());
                if (file.exists()) {
                    boolean deleted = file.delete();
                    log.info("删除本地备份文件 {} -> {}", file.getAbsolutePath(), deleted ? "成功" : "失败");
                }
            } else {
                try {
                    cosClient.deleteObject(cosConfig.getBucket(),
                            COS_BACKUP_PREFIX + backup.getFileUrl());
                    log.info("删除COS备份文件: {}", backup.getFileUrl());
                } catch (Exception e) {
                    log.warn("COS备份文件删除失败: {}", backup.getFileUrl(), e);
                }
            }
        }
        backupMapper.deleteById(backupId);
        log.info("删除备份记录 enterpriseId={}, backupId={}", enterpriseId, backupId);
    }

    /**
     * 使用原始 ID 通过 JDBC 直接插入（绕过 MyBatis-Plus IdType.AUTO，保持外键关联）
     */
    @SuppressWarnings("unchecked")
    private void insertListWithId(Map<String, Object> data, String key) {
        Object raw = data.get(key);
        if (raw == null) return;

        String tableName = TABLE_NAMES.get(key);
        if (tableName == null) {
            log.warn("未知的备份数据类型: {}", key);
            return;
        }

        List<Map<String, Object>> list = (List<Map<String, Object>>) raw;
        for (Map<String, Object> item : list) {
            item.remove("isDeleted");

            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            for (Map.Entry<String, Object> entry : item.entrySet()) {
                String col = camelToSnake(entry.getKey());
                if (!col.matches("[a-z][a-z0-9_]*")) {
                    log.warn("跳过非法列名: {}", col);
                    continue;
                }
                Object val = entry.getValue();
                // 处理 ISO 日期时间字符串中的 T 分隔符
                if (val instanceof String s && s.length() >= 19
                        && s.charAt(4) == '-' && s.charAt(10) == 'T') {
                    val = s.replace("T", " ");
                }
                columns.add(col);
                values.add(val);
            }

            StringJoiner colJoiner = new StringJoiner(", ");
            StringJoiner valJoiner = new StringJoiner(", ");
            for (String col : columns) {
                colJoiner.add("`" + col + "`");
                valJoiner.add("?");
            }
            String sql = "INSERT INTO `" + tableName + "` (" + colJoiner + ") VALUES (" + valJoiner + ")";
            jdbcTemplate.update(sql, values.toArray());
        }
        log.info("恢复 {} => {} 条记录", key, list.size());
    }

    /** 驼峰转下划线 */
    private static String camelToSnake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
