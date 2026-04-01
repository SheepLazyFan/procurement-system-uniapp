#!/bin/bash
# ==============================================================
# 采购系统后端一键部署脚本
# 功能：环境检查 → 本地 Maven 打包 → scp 上传 → 备份旧包 → 重启服务
# 用法：bash scripts/deploy.sh [--skip-build] [--setup-env]
# ==============================================================
set -euo pipefail

# ---- 配置区（按需修改） ----
SERVER="root@106.52.136.176"
DEPLOY_DIR="/opt/procurement"
JAR_NAME="procurement-server-1.0.0.jar"
SERVICE_NAME="procurement"
LOCAL_JAR="target/${JAR_NAME}"
ENV_FILE="${DEPLOY_DIR}/.env"

# Maven 路径（如 mvn 在 PATH 中可改为 mvn）
MVN_CMD="${MVN_CMD:-./mvnw}"

# ---- 颜色输出 ----
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ---- 解析参数 ----
SKIP_BUILD=false
SETUP_ENV=false
for arg in "$@"; do
  [[ "$arg" == "--skip-build" ]] && SKIP_BUILD=true
  [[ "$arg" == "--setup-env"  ]] && SETUP_ENV=true
done

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# ==========================
# Step 0: 服务器环境变量初始化
# 仅当 --setup-env 或 .env 不存在时执行
# ==========================
setup_server_env() {
  info "Step 0/4: 配置服务器环境变量..."
  ssh -T "$SERVER" bash <<'REMOTE_SETUP'
set -e
ENV_FILE="/opt/procurement/.env"

if [[ -f "$ENV_FILE" ]]; then
  echo "[SKIP] .env 已存在，跳过初始化（如需重置请手动删除 $ENV_FILE）"
  exit 0
fi

echo "[INFO] 创建 $ENV_FILE ..."
mkdir -p /opt/procurement

# 生成随机 JWT 密钥（32字节 hex）
JWT_SECRET_VAL=$(openssl rand -hex 32)

cat > "$ENV_FILE" <<ENV
# ============================================
# 采购系统生产环境密钥配置
# 此文件包含敏感信息，请勿提交至版本控制
# ============================================

# JWT 签名密钥（启动时自动生成，请勿清空）
JWT_SECRET=${JWT_SECRET_VAL}

# 微信小程序凭据（替换为正式凭据）
WX_APP_ID=wxb0ddba593d8a2497
WX_APP_SECRET=9d7e49f2ae110dcfde12c9c515efb37f

# 微信订阅消息模板 ID
WX_STOCK_WARNING_TEMPLATE_ID=ksHY5ef8GtwgegfA3Jz5Mab1vbjxghfitGlsMi1fzWc
WX_BACKUP_ALERT_TEMPLATE_ID=

# 数据库密码
DB_PASSWORD=Proc2026secure

# Redis 密码（无密码留空）
REDIS_PASSWORD=

# CORS 允许来源
CORS_ALLOWED_ORIGINS=https://servicewechat.com
ENV

chmod 600 "$ENV_FILE"
echo "[OK] $ENV_FILE 已创建（权限 600）"
REMOTE_SETUP

  # 确保 start.sh 会 source .env
  ssh -T "$SERVER" bash <<'REMOTE_PATCH'
set -e
START_SH="/opt/procurement/start.sh"

if [[ ! -f "$START_SH" ]]; then
  echo "[WARN] $START_SH 不存在，跳过 patch"
  exit 0
fi

# 避免重复注入
if grep -q "source /opt/procurement/.env" "$START_SH" 2>/dev/null; then
  echo "[SKIP] start.sh 已包含 source .env"
  exit 0
fi

# 在第一行 shebang 之后插入 source 语句
sed -i '1a\\n# 加载生产环境变量\n[ -f /opt/procurement/.env ] && set -a && source /opt/procurement/.env && set +a\n' "$START_SH"
echo "[OK] start.sh 已注入 source .env"
REMOTE_PATCH

  # 确保 systemd service 也能读取 .env（如果服务文件存在）
  ssh -T "$SERVER" bash <<'REMOTE_SYSTEMD'
set -e
SERVICE_FILE="/etc/systemd/system/procurement.service"

if [[ ! -f "$SERVICE_FILE" ]]; then
  echo "[SKIP] systemd service 文件不存在"
  exit 0
fi

if grep -q "EnvironmentFile" "$SERVICE_FILE"; then
  echo "[SKIP] EnvironmentFile 已配置"
  exit 0
fi

# 在 [Service] 段插入 EnvironmentFile
sed -i '/^\[Service\]/a EnvironmentFile=/opt/procurement/.env' "$SERVICE_FILE"
systemctl daemon-reload
echo "[OK] systemd EnvironmentFile 已配置，daemon 已 reload"
REMOTE_SYSTEMD

  info "Step 0/4: 环境变量配置完成"
}

# 判断是否需要执行 setup
if [[ "$SETUP_ENV" == "true" ]]; then
  setup_server_env
else
  # 自动检测：.env 是否存在
  ENV_EXISTS=$(ssh -T "$SERVER" "[[ -f '${ENV_FILE}' ]] && echo yes || echo no" 2>/dev/null || echo "no")
  if [[ "$ENV_EXISTS" == "no" ]]; then
    warn ".env 不存在，自动执行环境初始化..."
    setup_server_env
  fi
fi

# ==========================
# Step 1: 本地构建
# ==========================
if [[ "$SKIP_BUILD" == "false" ]]; then
  info "Step 1/4: Maven 打包（跳过测试）..."
  $MVN_CMD clean package -DskipTests -q \
    || error "Maven 构建失败，请检查代码"
  info "构建成功 → ${LOCAL_JAR}"
else
  warn "Step 1/4: 跳过构建（--skip-build）"
  [[ -f "$LOCAL_JAR" ]] || error "本地 JAR 不存在：${LOCAL_JAR}"
fi

# ==========================
# Step 2: 上传 JAR
# ==========================
info "Step 2/4: 上传 JAR 到服务器..."
scp -q "${LOCAL_JAR}" "${SERVER}:${DEPLOY_DIR}/${JAR_NAME}.new" \
  || error "SCP 上传失败，请检查 SSH 连接"
info "上传完成"

# ==========================
# Step 3: 服务器端操作（备份 + 替换 + 重启）
# ==========================
info "Step 3/4: 服务器端备份旧包并替换..."
ssh -T "$SERVER" bash <<REMOTE
set -e

# 备份旧 JAR（保留最近 5 个）
if [[ -f "${DEPLOY_DIR}/${JAR_NAME}" ]]; then
  cp "${DEPLOY_DIR}/${JAR_NAME}" "${DEPLOY_DIR}/backup/${JAR_NAME}.bak.${TIMESTAMP}" 2>/dev/null || true
  # 清理超过 5 个的旧备份
  ls -t "${DEPLOY_DIR}/backup/${JAR_NAME}.bak."* 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true
fi

# 替换新包
mv "${DEPLOY_DIR}/${JAR_NAME}.new" "${DEPLOY_DIR}/${JAR_NAME}"
echo "JAR 替换完成"
REMOTE

# ==========================
# Step 4: 重启服务
# ==========================
info "Step 4/4: 重启应用服务..."
ssh -T "$SERVER" bash <<REMOTE
set -e

if systemctl is-active --quiet "${SERVICE_NAME}"; then
  systemctl restart "${SERVICE_NAME}"
  echo "服务已重启"
else
  systemctl start "${SERVICE_NAME}" || true
  echo "服务已启动"
fi

# 等待启动最多 20 秒
for i in \$(seq 1 10); do
  sleep 2
  if curl -sf "http://127.0.0.1:8080/api/actuator/health" > /dev/null 2>&1; then
    echo "✅ 健康检查通过"
    exit 0
  fi
  echo "  等待服务就绪... (\${i}/10)"
done

echo "⚠️  健康检查超时，请手动确认：journalctl -u ${SERVICE_NAME} -n 50"
exit 1
REMOTE

info "======================================"
info "✅ 部署完成！${JAR_NAME}"
info "   服务器：${SERVER}"
info "   日志：${DEPLOY_DIR}/logs/app.log"
info "   API：http://106.52.136.176:8080/api"
info "======================================"
