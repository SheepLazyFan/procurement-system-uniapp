package com.procurement.config;

import java.util.List;

/**
 * Central catalog for manually maintained database schema migrations.
 * This project does not use Flyway/Liquibase yet, so startup checks and
 * deployment docs should share the same versioned source of truth.
 */
public final class DatabaseSchemaCatalog {

    private DatabaseSchemaCatalog() {}

    public static final String LATEST_SCHEMA_VERSION = "2026.04.07-01";

    public static final List<SchemaMigration> REQUIRED_MIGRATIONS = List.of(
            new SchemaMigration(
                    "2026.03.01-00",
                    "sql/init.sql",
                    "Baseline schema includes product QR code support",
                    List.of(new ColumnRequirement("pms_product", "qrcode_image"))
            ),
            new SchemaMigration(
                    "2026.03.12-01",
                    "sql/add_description.sql",
                    "Add product description field for richer product details",
                    List.of(new ColumnRequirement("pms_product", "description"))
            ),
            new SchemaMigration(
                    "2026.03.12-02",
                    "sql/add_order_source_cancel_by.sql",
                    "Add sales order source and cancel operator fields",
                    List.of(
                            new ColumnRequirement("oms_sales_order", "order_source"),
                            new ColumnRequirement("oms_sales_order", "cancel_by")
                    )
            ),
            new SchemaMigration(
                    "2026.04.07-01",
                    "sql/add_session_invalid_after.sql",
                    "Add enterprise session invalidation field for forced re-login",
                    List.of(new ColumnRequirement("sys_enterprise", "session_invalid_after"))
            )
    );

    public static final List<ColumnRequirement> REQUIRED_COLUMNS = List.of(
            new ColumnRequirement("pms_product", "qrcode_image"),
            new ColumnRequirement("pms_product", "description"),
            new ColumnRequirement("oms_sales_order", "order_source"),
            new ColumnRequirement("oms_sales_order", "cancel_by"),
            new ColumnRequirement("sys_enterprise", "session_invalid_after")
    );

    public static String requiredScriptSummary() {
        return REQUIRED_MIGRATIONS.stream()
                .map(migration -> migration.version() + "=" + migration.script())
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
    }

    public record SchemaMigration(
            String version,
            String script,
            String description,
            List<ColumnRequirement> requirements
    ) {}

    public record ColumnRequirement(String tableName, String columnName) {
        public String key() {
            return tableName + "." + columnName;
        }
    }
}
