package com.procurement.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseSchemaCompatibilityChecker - 数据库结构兼容检查")
class DatabaseSchemaCompatibilityCheckerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should pass startup check when all required columns exist")
    void should_passStartupCheck_when_allRequiredColumnsExist() throws Exception {
        // Arrange
        DatabaseSchemaCompatibilityChecker checker = new DatabaseSchemaCompatibilityChecker(jdbcTemplate);
        stubColumn("pms_product", "qrcode_image", 1);
        stubColumn("pms_product", "description", 1);
        stubColumn("oms_sales_order", "order_source", 1);
        stubColumn("oms_sales_order", "cancel_by", 1);
        stubColumn("sys_enterprise", "session_invalid_after", 1);

        // Act / Assert
        assertThatCode(() -> checker.run(new DefaultApplicationArguments(new String[0])))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should fail startup with actionable message when required column is missing")
    void should_failStartupWithActionableMessage_when_requiredColumnMissing() {
        // Arrange
        DatabaseSchemaCompatibilityChecker checker = new DatabaseSchemaCompatibilityChecker(jdbcTemplate);
        stubColumn("pms_product", "qrcode_image", 1);
        stubColumn("pms_product", "description", 1);
        stubColumn("oms_sales_order", "order_source", 1);
        stubColumn("oms_sales_order", "cancel_by", 1);
        stubColumn("sys_enterprise", "session_invalid_after", 0);

        // Act / Assert
        assertThatThrownBy(() -> checker.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("schemaVersion=" + DatabaseSchemaCatalog.LATEST_SCHEMA_VERSION)
                .hasMessageContaining("sys_enterprise.session_invalid_after")
                .hasMessageContaining("sql/add_session_invalid_after.sql");
    }

    private void stubColumn(String tableName, String columnName, int count) {
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(),
                eq(Integer.class), eq(tableName), eq(columnName)))
                .thenReturn(count);
    }
}
