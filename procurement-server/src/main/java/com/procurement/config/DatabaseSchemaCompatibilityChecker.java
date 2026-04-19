package com.procurement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动时做数据库结构兼容性检查，避免“JAR 已升级、生产库未跑 SQL”时拖到运行期才暴露。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaCompatibilityChecker implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "database_schema_expected schemaVersion={} requiredScripts={}",
                DatabaseSchemaCatalog.LATEST_SCHEMA_VERSION,
                DatabaseSchemaCatalog.requiredScriptSummary()
        );

        List<MissingColumn> missingColumns = findMissingColumns();
        if (!missingColumns.isEmpty()) {
            String detail = missingColumns.stream()
                    .map(req -> req.columnRequirement().key() + " <- " + req.sourceHint())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("unknown");
            log.error("database_schema_incompatible missingColumns={} action=STOP_STARTUP", detail);
            throw new IllegalStateException(
                    "数据库结构与当前后端版本不兼容，期望 schemaVersion="
                            + DatabaseSchemaCatalog.LATEST_SCHEMA_VERSION
                            + "，缺少字段：" + detail
                            + "。请先执行对应 SQL 升级脚本后再启动服务。");
        }

        log.info(
                "database_schema_check_passed schemaVersion={} checkedColumns={}",
                DatabaseSchemaCatalog.LATEST_SCHEMA_VERSION,
                DatabaseSchemaCatalog.REQUIRED_COLUMNS.size()
        );
    }

    List<MissingColumn> findMissingColumns() {
        List<MissingColumn> missing = new ArrayList<>();
        for (DatabaseSchemaCatalog.SchemaMigration migration : DatabaseSchemaCatalog.REQUIRED_MIGRATIONS) {
            for (DatabaseSchemaCatalog.ColumnRequirement requirement : migration.requirements()) {
                if (!columnExists(requirement.tableName(), requirement.columnName())) {
                    missing.add(new MissingColumn(requirement, migration.script()));
                }
            }
        }
        return missing;
    }

    boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    record MissingColumn(DatabaseSchemaCatalog.ColumnRequirement columnRequirement, String sourceHint) {}
}
