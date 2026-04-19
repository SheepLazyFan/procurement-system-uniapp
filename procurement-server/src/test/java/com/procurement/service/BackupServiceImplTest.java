package com.procurement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.response.BackupRestorePreviewResponse;
import com.procurement.entity.SysBackup;
import com.procurement.entity.SysEnterprise;
import com.procurement.mapper.*;
import com.procurement.service.impl.BackupServiceImpl;
import com.qcloud.cos.COSClient;
import com.procurement.config.CosConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackupServiceImpl - 备份恢复")
class BackupServiceImplTest {

    @Mock private BackupMapper backupMapper;
    @Mock private EnterpriseMapper enterpriseMapper;
    @Mock private UserMapper userMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ProductMapper productMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private SupplierMapper supplierMapper;
    @Mock private SalesOrderMapper salesOrderMapper;
    @Mock private SalesOrderItemMapper salesOrderItemMapper;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock private TeamMemberMapper teamMemberMapper;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private COSClient cosClient;
    @Mock private CosConfig cosConfig;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private BackupServiceImpl backupService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        setField("backupDir", tempDir.toString());
        setField("useLocalStorage", true);
    }

    // =================== previewRestore ===================

    @Test
    @DisplayName("Should preview warning when backup misses enterprise users or team state")
    void should_previewWarning_when_backupIsLegacy() throws Exception {
        SysBackup backup = buildBackup(11L, 1L, "legacy_backup.json");
        writeBackupFile("legacy_backup.json", Map.of(
                "backupId", 11L,
                "enterpriseId", 1L,
                "version", "1.0",
                "categories", List.of(),
                "products", List.of()
        ));

        SysEnterprise enterprise = new SysEnterprise();
        enterprise.setId(1L);
        enterprise.setName("当前企业");

        when(backupMapper.selectById(11L)).thenReturn(backup);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise);

        BackupRestorePreviewResponse preview = backupService.previewRestore(1L, 11L);

        assertThat(preview.getCurrentEnterpriseName()).isEqualTo("当前企业");
        assertThat(preview.getRecordCounts().get("enterprise")).isZero();
        assertThat(preview.getWarnings()).anyMatch(text -> text.contains("旧版本结构"));
    }

    @Test
    @DisplayName("Should preview valid backup with correct record counts")
    void should_returnCorrectPreview_when_backupIsValid() throws Exception {
        SysBackup backup = buildBackup(20L, 1L, "valid_backup.json");
        Map<String, Object> backupData = new LinkedHashMap<>();
        backupData.put("backupId", 20L);
        backupData.put("enterpriseId", 1L);
        backupData.put("version", "2.0");
        backupData.put("enterprise", Map.of("id", 1L, "name", "测试企业"));
        backupData.put("users", List.of(Map.of("id", 1L)));
        backupData.put("teamMembers", List.of(Map.of("id", 1L), Map.of("id", 2L)));
        backupData.put("categories", List.of(Map.of("id", 1L)));
        backupData.put("products", List.of(Map.of("id", 1L), Map.of("id", 2L)));
        backupData.put("customers", List.of());
        backupData.put("suppliers", List.of());
        backupData.put("salesOrders", List.of(Map.of("id", 1L)));
        backupData.put("salesOrderItems", List.of());
        backupData.put("purchaseOrders", List.of());
        backupData.put("purchaseOrderItems", List.of());
        writeBackupFile("valid_backup.json", backupData);

        SysEnterprise enterprise = new SysEnterprise();
        enterprise.setId(1L);
        enterprise.setName("当前企业");

        when(backupMapper.selectById(20L)).thenReturn(backup);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise);

        BackupRestorePreviewResponse preview = backupService.previewRestore(1L, 20L);

        assertThat(preview.getBackupEnterpriseName()).isEqualTo("测试企业");
        assertThat(preview.getRecordCounts().get("enterprise")).isEqualTo(1);
        assertThat(preview.getRecordCounts().get("users")).isEqualTo(1);
        assertThat(preview.getRecordCounts().get("teamMembers")).isEqualTo(2);
        assertThat(preview.getRecordCounts().get("products")).isEqualTo(2);
        assertThat(preview.getRecordCounts().get("salesOrders")).isEqualTo(1);
        assertThat(preview.isWillCreatePreRestoreSnapshot()).isTrue();
        assertThat(preview.isWillForceRelogin()).isTrue();
    }

    // =================== restore ===================

    @Test
    @DisplayName("Should reject restore when backup misses critical enterprise scope data")
    void should_rejectRestore_when_backupIsLegacy() throws Exception {
        SysBackup backup = buildBackup(12L, 1L, "legacy_restore.json");
        writeBackupFile("legacy_restore.json", Map.of(
                "backupId", 12L,
                "enterpriseId", 1L,
                "version", "1.0",
                "categories", List.of()
        ));

        when(backupMapper.selectById(12L)).thenReturn(backup);

        assertThatThrownBy(() -> backupService.restore(1L, 12L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.CONFLICT.getCode()));

        verify(backupMapper, never()).insert(any());
    }

    @Test
    @DisplayName("Should reject restore when backup belongs to different enterprise")
    void should_rejectRestore_when_enterpriseMismatch() throws Exception {
        SysBackup backup = buildBackup(30L, 1L, "cross_enterprise.json");
        Map<String, Object> backupData = new LinkedHashMap<>();
        backupData.put("backupId", 30L);
        backupData.put("enterpriseId", 999L);  // 不同企业
        backupData.put("version", "2.0");
        backupData.put("enterprise", Map.of("id", 999L, "name", "别人的企业"));
        backupData.put("users", List.of());
        backupData.put("teamMembers", List.of());
        backupData.put("categories", List.of());
        backupData.put("products", List.of());
        writeBackupFile("cross_enterprise.json", backupData);

        when(backupMapper.selectById(30L)).thenReturn(backup);

        assertThatThrownBy(() -> backupService.restore(1L, 30L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.CONFLICT.getCode()));
    }

    // =================== delete ===================

    @Test
    @DisplayName("Should delete backup record and local file")
    void should_deleteBackupAndFile_when_localStorage() throws Exception {
        // Create a local backup file
        String fileName = "delete_test.json";
        writeBackupFile(fileName, Map.of("test", true));

        SysBackup backup = buildBackup(40L, 1L, fileName);
        when(backupMapper.selectById(40L)).thenReturn(backup);

        backupService.delete(1L, 40L);

        verify(backupMapper).deleteById(40L);
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when deleting backup from another enterprise")
    void should_throwNotFound_when_deletingOtherEnterpriseBackup() {
        SysBackup backup = buildBackup(41L, 999L, "other.json");
        when(backupMapper.selectById(41L)).thenReturn(backup);

        assertThatThrownBy(() -> backupService.delete(1L, 41L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when backup does not exist")
    void should_throwNotFound_when_backupNotExists() {
        when(backupMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> backupService.delete(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }

    // =================== getBackupFile ===================

    @Test
    @DisplayName("Should throw NOT_FOUND when backup file does not exist on disk")
    void should_throwNotFound_when_backupFileNotExistsOnDisk() {
        SysBackup backup = buildBackup(50L, 1L, "nonexistent.json");
        when(backupMapper.selectById(50L)).thenReturn(backup);

        assertThatThrownBy(() -> backupService.getBackupFile(1L, 50L))
                .isInstanceOf(BusinessException.class);
    }

    // =================== 辅助方法 ===================

    private SysBackup buildBackup(Long backupId, Long enterpriseId, String fileName) {
        SysBackup backup = new SysBackup();
        backup.setId(backupId);
        backup.setEnterpriseId(enterpriseId);
        backup.setBackupType("FULL");
        backup.setStatus("COMPLETED");
        backup.setFileUrl(fileName);
        backup.setCreatedAt(LocalDateTime.of(2026, 4, 7, 12, 0));
        return backup;
    }

    private void writeBackupFile(String fileName, Map<String, Object> content) throws Exception {
        String backupDir = (String) getField("backupDir");
        Files.writeString(Path.of(backupDir, fileName), objectMapper.writeValueAsString(new LinkedHashMap<>(content)));
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = BackupServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(backupService, value);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = BackupServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(backupService);
    }
}
