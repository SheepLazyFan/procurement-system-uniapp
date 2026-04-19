# Database Migration Manifest

## Purpose

This project is still using manually executed SQL scripts.
Until Flyway or Liquibase is introduced, this file is the human-readable source of truth for:

- expected schema version
- required incremental scripts
- verification checkpoints after each release

Current expected schema version: `2026.04.07-01`

## Version History

| Version | Script | Purpose | Verify |
|------|------|------|------|
| `2026.03.01-00` | `sql/init.sql` | Baseline schema, including `pms_product.qrcode_image` | `SHOW COLUMNS FROM pms_product LIKE 'qrcode_image';` |
| `2026.03.12-01` | `sql/add_description.sql` | Add product description field | `SHOW COLUMNS FROM pms_product LIKE 'description';` |
| `2026.03.12-02` | `sql/add_order_source_cancel_by.sql` | Add `oms_sales_order.order_source` and `oms_sales_order.cancel_by` | `SHOW COLUMNS FROM oms_sales_order LIKE 'order_source';` and `SHOW COLUMNS FROM oms_sales_order LIKE 'cancel_by';` |
| `2026.04.07-01` | `sql/add_session_invalid_after.sql` | Add enterprise session invalidation field | `SHOW COLUMNS FROM sys_enterprise LIKE 'session_invalid_after';` |

## Release Rule

Before each backend release:

1. Check whether the new JAR expects a newer schema version.
2. Compare the target database with this manifest.
3. Back up the database.
4. Execute missing scripts manually.
5. Restart the backend.
6. Confirm startup logs contain `database_schema_check_passed`.

## Startup Log Contract

Backend startup should print:

- `database_schema_expected schemaVersion=...`
- `database_schema_check_passed schemaVersion=...`

If a required field is missing, startup should fail with:

- `database_schema_incompatible ... action=STOP_STARTUP`

This is intentional. It is better to fail at startup than to surface misleading runtime behavior such as token errors, blank pages, or fake "no enterprise" states.
