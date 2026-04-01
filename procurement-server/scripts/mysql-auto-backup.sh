#!/bin/bash
# =============================================================================
# MySQL 自动备份脚本 — 采购系统
# =============================================================================
#
# 功能：
#   1. 使用 mysqldump 生成带时间戳的完整数据库备份（SQL 格式）
#   2. 保留最近 KEEP_DAYS 天备份，自动清理旧文件
#   3. 打印结构化日志（时间戳 + 级别）
#   4. 任意步骤失败时返回非零退出码（供 crontab 邮件告警使用）
#
# 使用方式：
#   1. 将脚本上传到服务器，例如 /opt/procurement/scripts/mysql-auto-backup.sh
#   2. 赋予执行权限：chmod +x /opt/procurement/scripts/mysql-auto-backup.sh
#   3. 配置 crontab（sudo crontab -e）：
#
#      # 每天凌晨 3:00 执行 MySQL 备份，失败时发邮件（若配置了 MAILTO）
#      0 3 * * * /opt/procurement/scripts/mysql-auto-backup.sh >> /opt/procurement/logs/backup.log 2>&1
#
# 环境变量（可在 /etc/environment 或 ~/.bashrc 中设置）：
#   DB_HOST       数据库主机（默认 127.0.0.1）
#   DB_PORT       数据库端口（默认 3306）
#   DB_USERNAME   数据库用户名（默认 root）
#   DB_PASSWORD   数据库密码（必填，建议用 ~/.my.cnf 安全传递）
#   DB_NAME       数据库名称（默认 procurement_db）
#   BACKUP_DIR    备份存储目录（默认 /opt/procurement/backup/mysql）
#   KEEP_DAYS     保留天数（默认 7）
# =============================================================================

set -euo pipefail

# ─── 配置（可通过环境变量覆盖）────────────────────────────────────────────────
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-procurement_db}"
BACKUP_DIR="${BACKUP_DIR:-/opt/procurement/backup/mysql}"
KEEP_DAYS="${KEEP_DAYS:-7}"

# ─── 工具函数 ─────────────────────────────────────────────────────────────────
log_info()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] [INFO ] $*"; }
log_warn()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] [WARN ] $*"; }
log_error() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $*" >&2; }

# ─── 前置检查 ─────────────────────────────────────────────────────────────────
if ! command -v mysqldump &>/dev/null; then
    log_error "mysqldump 未找到，请确认 MySQL 客户端已安装"
    exit 1
fi

if [ -z "${DB_PASSWORD}" ]; then
    # 尝试从 ~/.my.cnf 读取，如未配置则报错
    if [ ! -f "${HOME}/.my.cnf" ]; then
        log_error "DB_PASSWORD 未设置，且 ~/.my.cnf 不存在。请设置环境变量或配置 ~/.my.cnf"
        exit 1
    fi
    log_info "DB_PASSWORD 为空，将使用 ~/.my.cnf 中的凭据"
fi

# ─── 创建备份目录 ─────────────────────────────────────────────────────────────
mkdir -p "${BACKUP_DIR}"

# ─── 执行备份 ─────────────────────────────────────────────────────────────────
TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"
BACKUP_FILE="${BACKUP_DIR}/procurement_db_${TIMESTAMP}.sql.gz"

log_info "===== 开始备份 ====="
log_info "数据库: ${DB_NAME}@${DB_HOST}:${DB_PORT}"
log_info "目标文件: ${BACKUP_FILE}"

if [ -n "${DB_PASSWORD}" ]; then
    MYSQL_PWD="${DB_PASSWORD}" mysqldump \
        --host="${DB_HOST}" \
        --port="${DB_PORT}" \
        --user="${DB_USERNAME}" \
        --single-transaction \
        --quick \
        --lock-tables=false \
        --routines \
        --triggers \
        --set-gtid-purged=OFF \
        "${DB_NAME}" | gzip -6 > "${BACKUP_FILE}"
else
    # 使用 ~/.my.cnf 中的凭据
    mysqldump \
        --host="${DB_HOST}" \
        --port="${DB_PORT}" \
        --user="${DB_USERNAME}" \
        --single-transaction \
        --quick \
        --lock-tables=false \
        --routines \
        --triggers \
        --set-gtid-purged=OFF \
        "${DB_NAME}" | gzip -6 > "${BACKUP_FILE}"
fi

# 验证备份文件非空
BACKUP_SIZE=$(stat -c%s "${BACKUP_FILE}" 2>/dev/null || echo 0)
if [ "${BACKUP_SIZE}" -lt 100 ]; then
    log_error "备份文件异常（大小 ${BACKUP_SIZE} 字节），可能为空或损坏"
    rm -f "${BACKUP_FILE}"
    exit 1
fi

log_info "备份完成: $(du -sh "${BACKUP_FILE}" | awk '{print $1}') (${BACKUP_FILE})"

# ─── 清理旧备份（保留最近 KEEP_DAYS 天）─────────────────────────────────────
log_info "清理 ${KEEP_DAYS} 天前的旧备份..."
DELETED_COUNT=0
while IFS= read -r old_file; do
    rm -f "${old_file}"
    log_info "  已删除: ${old_file}"
    DELETED_COUNT=$((DELETED_COUNT + 1))
done < <(find "${BACKUP_DIR}" -name "procurement_db_*.sql.gz" -mtime +"${KEEP_DAYS}" -type f)

if [ "${DELETED_COUNT}" -gt 0 ]; then
    log_info "共清理 ${DELETED_COUNT} 个过期备份文件"
else
    log_info "无过期备份文件需要清理"
fi

# ─── 统计信息 ─────────────────────────────────────────────────────────────────
TOTAL_FILES=$(find "${BACKUP_DIR}" -name "procurement_db_*.sql.gz" -type f | wc -l)
TOTAL_SIZE=$(du -sh "${BACKUP_DIR}" 2>/dev/null | awk '{print $1}')
log_info "当前备份目录: ${TOTAL_FILES} 个文件，共 ${TOTAL_SIZE}"
log_info "===== 备份任务完成 ====="
exit 0
