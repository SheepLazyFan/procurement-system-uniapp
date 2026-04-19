package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.response.BackupRestorePreviewResponse;
import com.procurement.entity.*;
import com.procurement.mapper.*;
import com.procurement.service.BackupService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.procurement.config.CosConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 数据备份服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private static final String BACKUP_TYPE_FULL = "FULL";
    private static final String BACKUP_TYPE_PRE_RESTORE = "PRE_RESTORE";
    private static final int PRE_RESTORE_RETAIN_COUNT = 10;
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String COS_BACKUP_PREFIX = "backup/";

    private static final Map<String, String> TABLE_NAMES = Map.ofEntries(
            Map.entry("categories", "pms_category"),
            Map.entry("products", "pms_product"),
            Map.entry("customers", "crm_customer"),
            Map.entry("suppliers", "crm_supplier"),
            Map.entry("salesOrders", "oms_sales_order"),
            Map.entry("salesOrderItems", "oms_sales_order_item"),
            Map.entry("purchaseOrders", "oms_purchase_order"),
            Map.entry("purchaseOrderItems", "oms_purchase_order_item"),
            Map.entry("teamMembers", "sys_team_member"),
            Map.entry("users", "sys_user")
    );

    private final BackupMapper backupMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final UserMapper userMapper;
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
    private final PlatformTransactionManager transactionManager;

    @Value("${backup.dir:backup}")
    private String backupDir;

    private final ObjectMapper objectMapper = createObjectMapper();

    private boolean useLocalStorage = false;

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
            log.warn("COS 凭证未配置，备份降级为本地存储 {}", backupDir);
        } else {
            log.info("备份使用腾讯云 COS 存储");
        }
    }

    @Override
    public SysBackup create(Long enterpriseId, String backupType) {
        return createBackupInternal(enterpriseId, normalizeBackupType(backupType), null, null, null);
    }

    @Override
    public List<SysBackup> list(Long enterpriseId) {
        return backupMapper.selectList(
                new LambdaQueryWrapper<SysBackup>()
                        .eq(SysBackup::getEnterpriseId, enterpriseId)
                        .orderByDesc(SysBackup::getId));
    }

    @Override
    public BackupRestorePreviewResponse previewRestore(Long enterpriseId, Long backupId) {
        SysBackup backup = getCompletedBackup(enterpriseId, backupId);
        Map<String, Object> data = readBackupData(backup);

        BackupRestorePreviewResponse response = new BackupRestorePreviewResponse();
        response.setBackupId(backup.getId());
        response.setBackupFileName(backup.getFileUrl());
        response.setBackupType(backup.getBackupType());
        response.setBackupCreatedAt(backup.getCreatedAt() != null
                ? backup.getCreatedAt().format(DISPLAY_TIME_FORMATTER)
                : "");
        response.setEnterpriseId(enterpriseId);

        SysEnterprise currentEnterprise = enterpriseMapper.selectById(enterpriseId);
        response.setCurrentEnterpriseName(currentEnterprise != null ? currentEnterprise.getName() : "");
        response.setBackupEnterpriseName(resolveEnterpriseName(data));
        response.setWillCreatePreRestoreSnapshot(true);
        response.setWillForceRelogin(true);
        response.setRecordCounts(buildRecordCounts(data));
        response.setWarnings(buildRestoreWarnings(data));
        return response;
    }

    @Override
    public void restore(Long enterpriseId, Long backupId, Long operatorUserId) {
        SysBackup backup = getCompletedBackup(enterpriseId, backupId);
        Map<String, Object> data = readBackupData(backup);
        validateRestorePayload(enterpriseId, data);

        SysBackup snapshot = createPreRestoreSnapshot(enterpriseId, backupId, operatorUserId);
        log.info("restore_pre_snapshot_created enterpriseId={} backupId={} snapshotId={}",
                enterpriseId, backupId, snapshot.getId());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            try {
                doRestoreTransactional(enterpriseId, data);
            } catch (RuntimeException e) {
                status.setRollbackOnly();
                throw e;
            }
        });

        invalidateEnterpriseSessions(enterpriseId);
        log.warn("restore_completed enterpriseId={} backupId={} operatorUserId={} action=FORCE_RELOGIN",
                enterpriseId, backupId, operatorUserId);
    }

    @Override
    public File getBackupFile(Long enterpriseId, Long backupId) {
        SysBackup backup = getCompletedBackup(enterpriseId, backupId);
        if (useLocalStorage) {
            File file = new File(backupDir, backup.getFileUrl());
            if (!file.exists()) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "备份文件不存在");
            }
            return file;
        }

        try {
            File tempFile = File.createTempFile("backup_download_", ".json");
            tempFile.deleteOnExit();
            cosClient.getObject(new GetObjectRequest(cosConfig.getBucket(),
                    COS_BACKUP_PREFIX + backup.getFileUrl()), tempFile);
            return tempFile;
        } catch (Exception e) {
            log.error("从 COS 下载备份文件失败: {}", backup.getFileUrl(), e);
            throw new BusinessException(ResultCode.FAIL.getCode(), "备份文件下载失败");
        }
    }

    @Override
    public void delete(Long enterpriseId, Long backupId) {
        SysBackup backup = backupMapper.selectById(backupId);
        if (backup == null || !Objects.equals(backup.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        deleteBackupStorage(backup);
        backupMapper.deleteById(backupId);
        log.info("删除备份记录 enterpriseId={}, backupId={}", enterpriseId, backupId);
    }

    private SysBackup createBackupInternal(Long enterpriseId, String backupType, String customRemark,
                                           Long sourceBackupId, Long operatorUserId) {
        SysBackup backup = new SysBackup();
        backup.setEnterpriseId(enterpriseId);
        backup.setBackupType(backupType);
        backup.setStatus("PROCESSING");
        backup.setFileUrl("");
        backup.setFileSize(0L);
        backup.setRemark(customRemark != null ? customRemark : "数据备份");
        backupMapper.insert(backup);

        try {
            Map<String, Object> data = collectBackupData(enterpriseId, backup, sourceBackupId, operatorUserId);
            String fileName = buildBackupFileName(backupType, enterpriseId, sourceBackupId);
            long fileSize = writeBackupFile(fileName, data);

            backup.setFileUrl(fileName);
            backup.setFileSize(fileSize);
            backup.setStatus("COMPLETED");
            backup.setRemark(buildBackupRemark(backupType, data, customRemark, sourceBackupId, operatorUserId));
            backupMapper.updateById(backup);

            if (BACKUP_TYPE_PRE_RESTORE.equals(backupType)) {
                cleanupExpiredPreRestoreBackups(enterpriseId);
            }

            log.info("backup_created enterpriseId={} backupId={} type={} file={} size={}",
                    enterpriseId, backup.getId(), backupType, fileName, fileSize);
            return backup;
        } catch (Exception e) {
            log.error("备份创建失败 enterpriseId={}, backupType={}", enterpriseId, backupType, e);
            backup.setStatus("FAILED");
            backup.setRemark("备份失败: " + e.getMessage());
            backupMapper.updateById(backup);
            throw new BusinessException(ResultCode.FAIL.getCode(), "备份失败: " + e.getMessage());
        }
    }

    private Map<String, Object> collectBackupData(Long enterpriseId, SysBackup backup,
                                                  Long sourceBackupId, Long operatorUserId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("backupId", backup.getId());
        data.put("enterpriseId", enterpriseId);
        data.put("backupType", backup.getBackupType());
        data.put("backupTime", LocalDateTime.now().format(DISPLAY_TIME_FORMATTER));
        data.put("version", "2.0");
        if (sourceBackupId != null) {
            data.put("sourceBackupId", sourceBackupId);
        }
        if (operatorUserId != null) {
            data.put("operatorUserId", operatorUserId);
        }

        data.put("enterprise", enterpriseMapper.selectById(enterpriseId));
        data.put("users", userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEnterpriseId, enterpriseId)));
        data.put("teamMembers", teamMemberMapper.selectList(
                new LambdaQueryWrapper<SysTeamMember>().eq(SysTeamMember::getEnterpriseId, enterpriseId)));
        data.put("categories", categoryMapper.selectList(
                new LambdaQueryWrapper<PmsCategory>().eq(PmsCategory::getEnterpriseId, enterpriseId)));
        data.put("products", productMapper.selectList(
                new LambdaQueryWrapper<PmsProduct>().eq(PmsProduct::getEnterpriseId, enterpriseId)));
        data.put("customers", customerMapper.selectList(
                new LambdaQueryWrapper<CrmCustomer>().eq(CrmCustomer::getEnterpriseId, enterpriseId)));
        data.put("suppliers", supplierMapper.selectList(
                new LambdaQueryWrapper<CrmSupplier>().eq(CrmSupplier::getEnterpriseId, enterpriseId)));

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

        return data;
    }

    private long writeBackupFile(String fileName, Map<String, Object> data) throws IOException {
        if (useLocalStorage) {
            File dir = new File(backupDir);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("无法创建备份目录: " + dir.getAbsolutePath());
            }
            File file = new File(dir, fileName);
            objectMapper.writeValue(file, data);
            return file.length();
        }

        byte[] jsonBytes = objectMapper.writeValueAsBytes(data);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(jsonBytes.length);
        metadata.setContentType("application/json; charset=utf-8");
        cosClient.putObject(new PutObjectRequest(cosConfig.getBucket(),
                COS_BACKUP_PREFIX + fileName,
                new ByteArrayInputStream(jsonBytes), metadata));
        return jsonBytes.length;
    }

    private String buildBackupFileName(String backupType, Long enterpriseId, Long sourceBackupId) {
        String now = LocalDateTime.now().format(FILE_TIME_FORMATTER);
        if (BACKUP_TYPE_PRE_RESTORE.equals(backupType)) {
            return String.format("pre_restore_enterprise_%d_from_%d_%s.json",
                    enterpriseId,
                    sourceBackupId != null ? sourceBackupId : 0L,
                    now);
        }
        return String.format("backup_%d_%s.json", enterpriseId, now);
    }

    private String buildBackupRemark(String backupType, Map<String, Object> data, String customRemark,
                                     Long sourceBackupId, Long operatorUserId) {
        if (customRemark != null && !customRemark.isBlank()) {
            return customRemark;
        }
        int totalRecords = buildRecordCounts(data).values().stream().mapToInt(Integer::intValue).sum();
        if (BACKUP_TYPE_PRE_RESTORE.equals(backupType)) {
            return String.format("恢复前自动快照，来源备份ID=%s，操作人=%s，共 %d 条记录",
                    sourceBackupId != null ? sourceBackupId : "-",
                    operatorUserId != null ? operatorUserId : "-",
                    totalRecords);
        }
        return "全量备份，共 " + totalRecords + " 条记录";
    }

    private SysBackup createPreRestoreSnapshot(Long enterpriseId, Long backupId, Long operatorUserId) {
        String remark = String.format("恢复前自动快照，来源备份ID=%d，操作人=%d", backupId, operatorUserId);
        return createBackupInternal(enterpriseId, BACKUP_TYPE_PRE_RESTORE, remark, backupId, operatorUserId);
    }

    private void cleanupExpiredPreRestoreBackups(Long enterpriseId) {
        List<SysBackup> snapshots = backupMapper.selectList(
                new LambdaQueryWrapper<SysBackup>()
                        .eq(SysBackup::getEnterpriseId, enterpriseId)
                        .eq(SysBackup::getBackupType, BACKUP_TYPE_PRE_RESTORE)
                        .orderByDesc(SysBackup::getId));
        if (snapshots.size() <= PRE_RESTORE_RETAIN_COUNT) {
            return;
        }
        for (int i = PRE_RESTORE_RETAIN_COUNT; i < snapshots.size(); i++) {
            SysBackup expired = snapshots.get(i);
            deleteBackupStorage(expired);
            backupMapper.deleteById(expired.getId());
        }
    }

    private void doRestoreTransactional(Long enterpriseId, Map<String, Object> data) {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        try {
            clearEnterpriseData(enterpriseId);

            insertObjectWithId(data, "enterprise", "sys_enterprise");
            insertListWithId(data, "users");
            insertListWithId(data, "teamMembers");
            insertListWithId(data, "categories");
            insertListWithId(data, "products");
            insertListWithId(data, "customers");
            insertListWithId(data, "suppliers");
            insertListWithId(data, "salesOrders");
            insertListWithId(data, "salesOrderItems");
            insertListWithId(data, "purchaseOrders");
            insertListWithId(data, "purchaseOrderItems");
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    private void clearEnterpriseData(Long enterpriseId) {
        jdbcTemplate.update("DELETE FROM `oms_sales_order_item` WHERE `order_id` IN (" +
                "SELECT `id` FROM `oms_sales_order` WHERE `enterprise_id` = ?)", enterpriseId);
        jdbcTemplate.update("DELETE FROM `oms_sales_order` WHERE `enterprise_id` = ?", enterpriseId);

        jdbcTemplate.update("DELETE FROM `oms_purchase_order_item` WHERE `order_id` IN (" +
                "SELECT `id` FROM `oms_purchase_order` WHERE `enterprise_id` = ?)", enterpriseId);
        jdbcTemplate.update("DELETE FROM `oms_purchase_order` WHERE `enterprise_id` = ?", enterpriseId);

        jdbcTemplate.update("DELETE FROM `sys_team_member` WHERE `enterprise_id` = ?", enterpriseId);
        jdbcTemplate.update("DELETE FROM `pms_product` WHERE `enterprise_id` = ?", enterpriseId);
        jdbcTemplate.update("DELETE FROM `pms_category` WHERE `enterprise_id` = ?", enterpriseId);
        jdbcTemplate.update("DELETE FROM `crm_customer` WHERE `enterprise_id` = ?", enterpriseId);
        jdbcTemplate.update("DELETE FROM `crm_supplier` WHERE `enterprise_id` = ?", enterpriseId);
        jdbcTemplate.update("DELETE FROM `sys_user` WHERE `enterprise_id` = ?", enterpriseId);
        jdbcTemplate.update("DELETE FROM `sys_enterprise` WHERE `id` = ?", enterpriseId);
    }

    private void invalidateEnterpriseSessions(Long enterpriseId) {
        LocalDateTime now = LocalDateTime.now();
        enterpriseMapper.update(null, new LambdaUpdateWrapper<SysEnterprise>()
                .eq(SysEnterprise::getId, enterpriseId)
                .set(SysEnterprise::getSessionInvalidAfter, now));
    }

    private SysBackup getCompletedBackup(Long enterpriseId, Long backupId) {
        SysBackup backup = backupMapper.selectById(backupId);
        if (backup == null || !Objects.equals(backup.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!"COMPLETED".equals(backup.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "备份状态异常，无法执行该操作");
        }
        return backup;
    }

    private Map<String, Object> readBackupData(SysBackup backup) {
        try {
            if (useLocalStorage) {
                File file = new File(backupDir, backup.getFileUrl());
                if (!file.exists()) {
                    throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "备份文件不存在，可能已被删除");
                }
                return objectMapper.readValue(file, new TypeReference<>() {});
            }

            try (InputStream is = cosClient.getObject(cosConfig.getBucket(),
                    COS_BACKUP_PREFIX + backup.getFileUrl()).getObjectContent()) {
                return objectMapper.readValue(is, new TypeReference<>() {});
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取备份文件失败 backupId={}, file={}", backup.getId(), backup.getFileUrl(), e);
            throw new BusinessException(ResultCode.FAIL.getCode(), "读取备份文件失败: " + e.getMessage());
        }
    }

    private void validateRestorePayload(Long enterpriseId, Map<String, Object> data) {
        Long payloadEnterpriseId = toLong(data.get("enterpriseId"));
        if (payloadEnterpriseId == null || !Objects.equals(payloadEnterpriseId, enterpriseId)) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "备份所属企业与当前企业不一致");
        }

        if (!(data.get("enterprise") instanceof Map<?, ?>) || data.get("users") == null || data.get("teamMembers") == null) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "该备份缺少企业资料、用户归属或团队状态，不能执行完整恢复");
        }
    }

    private Map<String, Integer> buildRecordCounts(Map<String, Object> data) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("enterprise", data.get("enterprise") instanceof Map<?, ?> ? 1 : 0);
        counts.put("users", sizeOfList(data.get("users")));
        counts.put("teamMembers", sizeOfList(data.get("teamMembers")));
        counts.put("categories", sizeOfList(data.get("categories")));
        counts.put("products", sizeOfList(data.get("products")));
        counts.put("customers", sizeOfList(data.get("customers")));
        counts.put("suppliers", sizeOfList(data.get("suppliers")));
        counts.put("salesOrders", sizeOfList(data.get("salesOrders")));
        counts.put("salesOrderItems", sizeOfList(data.get("salesOrderItems")));
        counts.put("purchaseOrders", sizeOfList(data.get("purchaseOrders")));
        counts.put("purchaseOrderItems", sizeOfList(data.get("purchaseOrderItems")));
        return counts;
    }

    private List<String> buildRestoreWarnings(Map<String, Object> data) {
        List<String> warnings = new ArrayList<>();
        warnings.add("恢复前会自动生成一份当前数据快照，作为回滚兜底。");
        warnings.add("恢复成功后，该企业下所有商家和员工都需要重新登录。");
        warnings.add("本次恢复会覆盖企业资料、用户归属、团队状态和业务数据。");
        if (!(data.get("enterprise") instanceof Map<?, ?>) || data.get("users") == null || data.get("teamMembers") == null) {
            warnings.add("该备份是旧版本结构，缺少完整恢复所需的关键数据，正式恢复会被拒绝。");
        }
        return warnings;
    }

    private String resolveEnterpriseName(Map<String, Object> data) {
        if (data.get("enterprise") instanceof Map<?, ?> enterprise) {
            Object name = enterprise.get("name");
            return name != null ? name.toString() : "";
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private void insertObjectWithId(Map<String, Object> data, String key, String tableName) {
        Object raw = data.get(key);
        if (!(raw instanceof Map<?, ?> item)) {
            return;
        }
        insertRowWithId((Map<String, Object>) item, tableName);
    }

    @SuppressWarnings("unchecked")
    private void insertListWithId(Map<String, Object> data, String key) {
        Object raw = data.get(key);
        if (!(raw instanceof List<?> list)) {
            return;
        }

        String tableName = TABLE_NAMES.get(key);
        if (tableName == null) {
            log.warn("未知的备份数据类型 key={}", key);
            return;
        }

        for (Object item : list) {
            if (item instanceof Map<?, ?> row) {
                insertRowWithId((Map<String, Object>) row, tableName);
            }
        }
    }

    private void insertRowWithId(Map<String, Object> item, String tableName) {
        Map<String, Object> sanitized = new LinkedHashMap<>(item);
        sanitized.remove("isDeleted");

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> entry : sanitized.entrySet()) {
            String col = camelToSnake(entry.getKey());
            if (!col.matches("[a-z][a-z0-9_]*")) {
                log.warn("跳过非法列名 table={}, col={}", tableName, col);
                continue;
            }
            columns.add("`" + col + "`");
            values.add(normalizeColumnValue(entry.getValue()));
        }

        if (columns.isEmpty()) {
            return;
        }

        StringJoiner columnJoiner = new StringJoiner(", ");
        StringJoiner valueJoiner = new StringJoiner(", ");
        for (String column : columns) {
            columnJoiner.add(column);
            valueJoiner.add("?");
        }

        String sql = "INSERT INTO `" + tableName + "` (" + columnJoiner + ") VALUES (" + valueJoiner + ")";
        jdbcTemplate.update(sql, values.toArray());
    }

    private Object normalizeColumnValue(Object value) {
        if (value instanceof String text
                && text.length() >= 19
                && text.charAt(4) == '-'
                && text.charAt(10) == 'T') {
            return text.replace("T", " ");
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                throw new BusinessException(ResultCode.FAIL.getCode(), "序列化备份字段失败: " + e.getMessage());
            }
        }
        return value;
    }

    private void deleteBackupStorage(SysBackup backup) {
        if (backup.getFileUrl() == null || backup.getFileUrl().isBlank()) {
            return;
        }
        if (useLocalStorage) {
            File file = new File(backupDir, backup.getFileUrl());
            if (file.exists() && !file.delete()) {
                log.warn("删除本地备份文件失败: {}", file.getAbsolutePath());
            }
            return;
        }
        try {
            cosClient.deleteObject(cosConfig.getBucket(), COS_BACKUP_PREFIX + backup.getFileUrl());
        } catch (Exception e) {
            log.warn("删除 COS 备份文件失败: {}", backup.getFileUrl(), e);
        }
    }

    private String normalizeBackupType(String backupType) {
        return backupType == null || backupType.isBlank() ? BACKUP_TYPE_FULL : backupType.trim().toUpperCase();
    }

    private int sizeOfList(Object value) {
        return value instanceof List<?> list ? list.size() : 0;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static String camelToSnake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
