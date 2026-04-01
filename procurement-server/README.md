# 采购系统后端 (procurement-server)

基于 Spring Boot 3.2.3 + MyBatis-Plus 的微信小程序采购管理系统后端服务。

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.3, Spring Security |
| ORM | MyBatis-Plus 3.5.x |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 6.x |
| 认证 | JWT (JJWT) |
| 构建 | Maven 3.9 + JDK 21 |
| 运行时 | Ubuntu 22.04, systemd |

---

## 本地开发

### 前提条件

- JDK 21
- MySQL 8.0（本地，端口 3306）
- Redis（本地，端口 6379）

### 启动步骤

```bash
# 1. 初始化数据库
mysql -u root -p < sql/schema.sql

# 2. 启动应用（使用 dev profile，连接本地 MySQL/Redis）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

应用启动后访问：
- API 基础路径：`http://localhost:8080/api`
- Swagger 文档：`http://localhost:8080/api/swagger-ui.html`

---

## 生产部署

### 一、服务器环境准备

```bash
# 创建目录结构
mkdir -p /opt/procurement/{logs,data/image,backup}

# 安装 JDK 21
apt install -y openjdk-21-jdk-headless

# 确认 MySQL、Redis 已启动
systemctl status mysql
systemctl status redis-server
```

### 二、配置环境变量

将所有必填变量写入 `/opt/procurement/.env`（**不要提交到 Git**）：

```bash
cp .env.example .env
# 编辑填写真实值
nano /opt/procurement/.env
```

`.env` 文件示例内容见 `.env.example`，至少必须设置：

| 变量 | 说明 |
|------|------|
| `JWT_SECRET` | JWT 签名密钥，生产环境必须设置强密钥 |
| `WX_APP_ID` | 微信小程序 AppID |
| `WX_APP_SECRET` | 微信小程序 AppSecret |
| `DB_PASSWORD` | MySQL 数据库密码 |

### 三、注册 systemd 服务

创建 `/etc/systemd/system/procurement.service`：

```ini
[Unit]
Description=Procurement System Backend
After=network.target mysql.service redis-server.service
Requires=mysql.service redis-server.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/procurement
EnvironmentFile=/opt/procurement/.env
ExecStart=/usr/bin/java \
  -Xms256m -Xmx512m \
  -Dspring.profiles.active=prod \
  -Dlogging.file.path=/opt/procurement/logs \
  -jar /opt/procurement/procurement-server-1.0.0.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
# 加载并启动
systemctl daemon-reload
systemctl enable procurement
systemctl start procurement

# 确认状态
systemctl status procurement
journalctl -u procurement -f
```

### 四、一键部署（更新版本）

```bash
# 在本地项目目录执行（需要 SSH 免密登录服务器）
bash scripts/deploy.sh

# 如果已有编译好的 JAR，跳过构建：
bash scripts/deploy.sh --skip-build
```

> **要求**：本地需要 JDK 21 + Maven，且 SSH 已配置免密登录到服务器。

---

## HTTPS 部署（微信小程序正式版必须）

> 微信小程序正式版要求所有 API 域名使用 HTTPS。以下为完整配置步骤。

### 前提条件

- ✅ 已备案域名（解析到服务器 IP）
- ✅ SSL 证书（腾讯云免费证书 / Let's Encrypt）

### 一、安装 Nginx

```bash
apt update && apt install -y nginx
systemctl enable nginx
```

### 二、配置 SSL 证书

```bash
# 创建证书目录
mkdir -p /etc/nginx/ssl

# 将证书文件上传到服务器（腾讯云下载的 .crt 和 .key 文件）
# /etc/nginx/ssl/your-domain.crt
# /etc/nginx/ssl/your-domain.key
```

### 三、Nginx 反向代理配置

创建 `/etc/nginx/sites-available/procurement`：

```nginx
# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 主配置
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 证书
    ssl_certificate     /etc/nginx/ssl/your-domain.crt;
    ssl_certificate_key /etc/nginx/ssl/your-domain.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # 反向代理到 Spring Boot
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 图片文件直接代理
    location /api/files/ {
        proxy_pass http://127.0.0.1:8080/api/files/;
        proxy_set_header Host $host;
    }
}
```

```bash
# 启用配置
ln -s /etc/nginx/sites-available/procurement /etc/nginx/sites-enabled/
nginx -t           # 语法检查
systemctl reload nginx
```

### 四、防火墙放行 443

```bash
ufw allow 443/tcp
# 腾讯云轻量需同步在控制台 → 防火墙 添加 HTTPS (443) 规则
```

### 五、微信后台配置

1. 登录 [微信公众平台](https://mp.weixin.qq.com)
2. 开发管理 → 开发设置 → 服务器域名
3. request 合法域名添加：`https://your-domain.com`

### 六、更新前端 API 地址

```bash
# procurement-uniapp/.env.production
VITE_API_BASE=https://your-domain.com/api
```

重新构建并上传微信小程序。

---

## 环境变量完整说明

| 变量 | 默认值 | 必填 | 说明 |
|------|--------|------|------|
| `JWT_SECRET` | 无 | ✅ | JWT 签名密钥（32+ 字符） |
| `WX_APP_ID` | 无 | ✅ | 微信小程序 AppID |
| `WX_APP_SECRET` | 无 | ✅ | 微信小程序 AppSecret |
| `WX_STOCK_WARNING_TEMPLATE_ID` | 空 | 否 | 低库存预警订阅消息模板 ID |
| `WX_BACKUP_ALERT_TEMPLATE_ID` | 空 | 否 | 备份失败告警订阅消息模板 ID |
| `DB_HOST` | `127.0.0.1` | 否 | MySQL 主机 |
| `DB_PORT` | `3306` | 否 | MySQL 端口 |
| `DB_USERNAME` | `root` | 否 | MySQL 用户名 |
| `DB_PASSWORD` | 无 | ✅ | MySQL 密码 |
| `REDIS_HOST` | `127.0.0.1` | 否 | Redis 主机 |
| `REDIS_PORT` | `6379` | 否 | Redis 端口 |
| `REDIS_PASSWORD` | 空 | 否 | Redis 密码（无密码则留空） |
| `FILE_LOCAL_DIR` | `data/image` | 否 | 本地图片存储目录 |
| `COS_SECRET_ID` | 空 | 否 | 腾讯云 COS SecretId |
| `COS_SECRET_KEY` | 空 | 否 | 腾讯云 COS SecretKey |
| `COS_BUCKET` | 空 | 否 | COS Bucket 名称 |
| `BACKUP_DIR` | `backup` | 否 | 数据备份存储目录 |
| `BACKUP_AUTO_CRON` | `0 0 3 * * ?` | 否 | 自动备份 Cron 表达式 |
| `CORS_ALLOWED_ORIGINS` | `*` | 否 | CORS 允许来源 |

---

## 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE procurement_db DEFAULT CHARSET utf8mb4;"

# 执行建表脚本
mysql -u root -p procurement_db < sql/schema.sql
```

---

## 数据库自动备份

系统内置每日凌晨 3 点自动备份（Spring `@Scheduled`），备份文件存储在 `BACKUP_DIR`。

```bash
# 安装 mysqldump 脚本
cp scripts/mysql-auto-backup.sh /opt/procurement/scripts/
chmod +x /opt/procurement/scripts/mysql-auto-backup.sh

# 配置 MySQL 免密
cp scripts/my.cnf.example ~/.my.cnf
chmod 600 ~/.my.cnf && nano ~/.my.cnf

# 加入 crontab（每天 3:30 执行）
crontab -e
# 添加: 30 3 * * * /opt/procurement/scripts/mysql-auto-backup.sh >> /opt/procurement/logs/backup.log 2>&1
```

---

## 测试

```bash
# 运行全部 77 个 JUnit 测试
./mvnw test

# 运行指定测试类
./mvnw test -Dtest=BuyerServiceImplTest
```

| 测试类 | 用例数 | 说明 |
|--------|--------|------|
| AuthServiceImplTest | 10 | 微信登录/注册/角色 |
| ProductServiceImplTest | 15 | 商品 CRUD/上下架/库存 |
| BuyerServiceImplTest | 15 | 买家端/库存阈值(T-01~T-07) |
| SalesOrderServiceImplTest | 19 | 销售订单全流程 |
| PurchaseOrderServiceImplTest | 12 | 采购订单/到货入库 |
| AdjustStockRbacTest | 5 | RBAC 权限验证(T-08~T-11) |
| ProcurementApplicationTests | 1 | Spring 上下文启动 |
| **合计** | **77** | **BUILD SUCCESS ✅** |

---

## API 文档示例

> 开发环境启动后访问 Swagger UI：`http://localhost:8080/api/swagger-ui.html`

### 微信登录

```
POST /api/auth/wx-login
Content-Type: application/json

{ "code": "0b3kVe000..." }
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOi...",
    "userInfo": { "id": 1, "role": "SELLER", "enterpriseId": 1 }
  }
}
```

### 商品列表（需 Token）

```
GET /api/products?pageNum=1&pageSize=20&categoryId=1
Authorization: Bearer <token>
```

### 库存调整（需 SELLER/ADMIN/WAREHOUSE 权限）

```
PUT /api/products/1/stock
Authorization: Bearer <token>
Content-Type: application/json

{ "adjustAmount": 50 }
```

---

## 常用运维命令

```bash
# 查看服务状态
systemctl status procurement

# 查看实时日志
journalctl -u procurement -f

# 重启应用
systemctl restart procurement

# 查看最近备份
ls -lh /opt/procurement/backup/

# 检查 API 健康状态
curl http://127.0.0.1:8080/api/actuator/health
```

---

## 目录结构

```
procurement-server/
├── src/
│   ├── main/
│   │   ├── java/com/procurement/
│   │   │   ├── controller/     # REST 控制器
│   │   │   ├── service/        # 业务逻辑
│   │   │   ├── mapper/         # MyBatis-Plus 数据访问
│   │   │   ├── entity/         # 数据库实体
│   │   │   ├── dto/            # 请求/响应 DTO
│   │   │   ├── security/       # JWT + Spring Security
│   │   │   └── common/         # 工具类、异常、常量
│   │   └── resources/
│   │       ├── application.yml          # 公共配置
│   │       ├── application-dev.yml      # 开发环境配置
│   │       └── application-prod.yml     # 生产环境配置
│   └── test/                   # 77 个 JUnit 测试
├── sql/                        # 数据库脚本
├── scripts/
│   ├── deploy.sh               # 一键部署脚本
│   ├── mysql-auto-backup.sh    # MySQL 备份脚本
│   └── my.cnf.example          # MySQL 免密配置模板
├── .env.example                # 环境变量配置模板
└── pom.xml
```
