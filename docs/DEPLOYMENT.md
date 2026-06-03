# DreamArchive 部署文档

## 目录

1. [环境要求](#1-环境要求)
2. [服务器准备](#2-服务器准备)
3. [数据库部署](#3-数据库部署)
4. [Redis 部署](#4-redis-部署)
5. [MinIO 部署](#5-minio-部署)
6. [后端部署](#6-后端部署)
7. [前端部署](#7-前端部署)
8. [Nginx 配置](#8-nginx-配置)
9. [生产环境配置](#9-生产环境配置)
10. [HTTPS 配置（可选）](#10-https-配置可选)
11. [运维与监控](#11-运维与监控)
12. [常见问题](#12-常见问题)

---

## 1. 环境要求

### 基础环境

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | Spring Boot 3.5.7 最低要求 Java 17 |
| Maven | 3.8+ | 后端构建工具 |
| Node.js | 18+ | 前端构建工具 |
| npm | 9+ | 前端包管理 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 验证码 & 缓存 |
| MinIO | 最新稳定版 | 对象存储（图片/头像） |
| Nginx | 1.20+ | 反向代理 & 前端静态资源 |

### 硬件建议（最低配置）

| 资源 | 最低 | 推荐 |
|------|------|------|
| CPU | 2 核 | 4 核 |
| 内存 | 2 GB | 4 GB |
| 磁盘 | 20 GB | 50 GB SSD |
| 带宽 | 2 Mbps | 5 Mbps |

---

## 2. 服务器准备

### 2.1 系统初始化（CentOS 7 / 8）

```bash
# 更新系统
yum update -y

# 安装常用工具
yum install -y wget curl vim net-tools unzip

# 关闭 SELinux（可选，建议生产环境配置正确的策略）
setenforce 0
sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config

# 配置防火墙，放行所需端口
firewall-cmd --permanent --add-port=80/tcp      # HTTP
firewall-cmd --permanent --add-port=443/tcp     # HTTPS
firewall-cmd --permanent --add-port=8080/tcp    # 后端 API
firewall-cmd --permanent --add-port=3306/tcp    # MySQL
firewall-cmd --permanent --add-port=6379/tcp    # Redis
firewall-cmd --permanent --add-port=9000/tcp    # MinIO API
firewall-cmd --permanent --add-port=9001/tcp    # MinIO Console
firewall-cmd --reload
```

### 2.2 安装 JDK 17

```bash
# 方式一：yum 安装 OpenJDK
yum install -y java-17-openjdk java-17-openjdk-devel

# 方式二：手动安装 Oracle JDK（本地开发路径参考）
# 下载 jdk-17_linux-x64_bin.tar.gz
tar -xzf jdk-17_linux-x64_bin.tar.gz -C /usr/local/
echo 'export JAVA_HOME=/usr/local/jdk-17' >> /etc/profile
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile
source /etc/profile

# 验证
java -version
```

### 2.3 安装 Maven

```bash
yum install -y maven
# 或手动安装
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar -xzf apache-maven-3.9.6-bin.tar.gz -C /usr/local/
echo 'export M2_HOME=/usr/local/apache-maven-3.9.6' >> /etc/profile
echo 'export PATH=$M2_HOME/bin:$PATH' >> /etc/profile
source /etc/profile

mvn -version
```

### 2.4 安装 Node.js（前端构建用）

```bash
# 使用 NodeSource 安装 Node.js 18
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs

# 验证
node -v
npm -v
```

---

## 3. 数据库部署

### 3.1 安装 MySQL 8.0

```bash
# 添加 MySQL 官方 YUM 源
rpm -Uvh https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm
yum install -y mysql-community-server

# 启动并设置开机自启
systemctl start mysqld
systemctl enable mysqld

# 获取临时密码
grep 'temporary password' /var/log/mysqld.log

# 安全初始化
mysql_secure_installation
```

### 3.2 创建数据库和表

```bash
# 登录 MySQL
mysql -u root -p
```

```sql
-- 创建数据库
CREATE DATABASE dream DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 使用数据库
USE dream;

-- 执行建表 SQL（完整版，包含所有字段）
-- 以下为完整建表语句：

-- 用户表
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'USER',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'ACTIVE',
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  `avatar_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '头像对象路径',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `username`(`username`),
  UNIQUE INDEX `email`(`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- 梦境内容表
CREATE TABLE `dream_content` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'UUID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '梦境标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '梦境内容',
  `emotion` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '情绪标签',
  `place` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '梦境地点',
  `time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '梦境时间',
  `interpretation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'AI解析结果',
  `image_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '梦境图片对象路径',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id`(`user_id`),
  INDEX `idx_created_at`(`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 梦境统计汇总表
CREATE TABLE `dream_stats` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_dreams` int DEFAULT 0 COMMENT '总梦境数',
  `happy_count` int DEFAULT 0 COMMENT '开心梦境数',
  `sad_count` int DEFAULT 0 COMMENT '难过梦境数',
  `scared_count` int DEFAULT 0 COMMENT '恐惧梦境数',
  `angry_count` int DEFAULT 0 COMMENT '愤怒梦境数',
  `peaceful_count` int DEFAULT 0 COMMENT '平静梦境数',
  `other_emotion_count` int DEFAULT 0 COMMENT '其他情绪梦境数',
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_date`(`user_id`, `stat_date`),
  INDEX `idx_user_id`(`user_id`),
  INDEX `idx_stat_date`(`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 梦境地点统计表
CREATE TABLE `dream_place_stats` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `place` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '地点',
  `dream_count` int DEFAULT 0 COMMENT '梦境数量',
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_place`(`user_id`, `place`),
  INDEX `idx_user_id`(`user_id`),
  INDEX `idx_dream_count`(`dream_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 设置第一个注册用户为超级管理员（在应用启动后，第一个注册的用户会自动成为 SUPER_ADMIN）
-- 如需手动指定管理员：
-- UPDATE user SET role = 'ADMIN' WHERE email = 'your-email@example.com';
```

> **说明**：也可以直接导入项目中的 `sql/init.sql` 文件：
> ```bash
> mysql -u root -p dream < sql/init.sql
> ```
> 然后执行补丁 SQL 添加后续新增字段：
> ```bash
> mysql -u root -p dream < src/main/resources/db/admin_role_patch.sql
> mysql -u root -p dream < src/main/resources/db/admin_user_status_patch.sql
> ```

### 3.3 创建专用数据库用户（推荐）

```sql
-- 创建应用专用用户，避免使用 root
CREATE USER 'dream_app'@'%' IDENTIFIED BY 'YourStrongPassword123!';
GRANT SELECT, INSERT, UPDATE, DELETE ON dream.* TO 'dream_app'@'%';
FLUSH PRIVILEGES;
```

---

## 4. Redis 部署

### 4.1 安装 Redis

```bash
# CentOS 7
yum install -y epel-release
yum install -y redis

# 启动并设置开机自启
systemctl start redis
systemctl enable redis
```

### 4.2 配置 Redis

编辑 `/etc/redis.conf`（或 `/etc/redis/redis.conf`）：

```conf
# 绑定地址（允许远程连接）
bind 0.0.0.0

# 设置密码（强烈推荐）
requirepass YourRedisPassword

# 持久化
appendonly yes
appendfsync everysec

# 最大内存限制
maxmemory 256mb
maxmemory-policy allkeys-lru
```

```bash
# 重启生效
systemctl restart redis

# 验证连接
redis-cli -h 127.0.0.1 -p 6379 -a YourRedisPassword ping
```

### 4.3 防火墙放行

```bash
firewall-cmd --permanent --add-port=6379/tcp
firewall-cmd --reload
```

---

## 5. MinIO 部署

### 5.1 安装 MinIO

```bash
# 下载 MinIO
wget https://dl.min.io/server/minio/release/linux-amd64/minio
chmod +x minio
mv minio /usr/local/bin/

# 创建数据目录
mkdir -p /data/minio

# 创建专用用户
useradd -r minio-user -s /sbin/nologin
chown -R minio-user:minio-user /data/minio
```

### 5.2 配置 Systemd 服务

创建 `/etc/systemd/system/minio.service`：

```ini
[Unit]
Description=MinIO
Documentation=https://docs.min.io
After=network.target

[Service]
User=minio-user
Group=minio-user
Environment="MINIO_ROOT_USER=your-minio-access-key"
Environment="MINIO_ROOT_PASSWORD=your-minio-secret-key"
ExecStart=/usr/local/bin/minio server /data/minio --address ":9000" --console-address ":9001"
Restart=always
RestartSec=10
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl start minio
systemctl enable minio
```

### 5.3 创建 Bucket

访问 MinIO Console：`http://服务器IP:9001`

1. 使用配置的 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` 登录
2. 创建 Bucket：`dream-archive`
3. 设置 Bucket 访问策略为 `public`（或配置自定义策略）

或使用 MinIO Client (mc)：

```bash
# 安装 mc
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc
mv mc /usr/local/bin/

# 配置别名
mc alias set myminio http://localhost:9000 your-minio-access-key your-minio-secret-key

# 创建 Bucket
mc mb myminio/dream-archive

# 设置公开读取策略（用于图片访问）
mc anonymous set download myminio/dream-archive
```

---

## 6. 后端部署

### 6.1 获取代码

```bash
# 克隆仓库
git clone <your-repo-url> /opt/dream-archive
cd /opt/dream-archive
```

### 6.2 配置生产环境

创建生产环境配置文件 `src/main/resources/application-prod.properties`：

```properties
# ===== 数据库 =====
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/dream?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=dream_app
spring.datasource.password=YourStrongPassword123!
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ===== MyBatis =====
mybatis.type-aliases-package=com.yiweilai.DreamArchive.DTO.Dream
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.configuration.map-underscore-to-camel-case=true

# ===== Redis =====
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.database=0
spring.data.redis.password=YourRedisPassword

# ===== 邮件 (QQ邮箱 SMTP) =====
spring.mail.host=smtp.qq.com
spring.mail.port=587
spring.mail.username=your-qq-email@qq.com
spring.mail.password=your-smtp-authorization-code
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# ===== 文件上传 =====
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB

# ===== MinIO =====
minio.endpoint=http://127.0.0.1:9000
minio.access-key=your-minio-access-key
minio.secret-key=your-minio-secret-key
minio.bucket=dream-archive

# ===== AI Provider Pool =====
ai.pool.providers[0].name=mimo
ai.pool.providers[0].url=https://token-plan-cn.xiaomimimo.com/v1
ai.pool.providers[0].apiKey=your-api-key
ai.pool.providers[0].model=mimo-v2.5-pro
ai.pool.providers[0].weight=50
ai.pool.providers[0].enabled=true

ai.pool.providers[1].name=mimo2
ai.pool.providers[1].url=https://token-plan-cn.xiaomimimo.com/v1
ai.pool.providers[1].apiKey=your-api-key
ai.pool.providers[1].model=mimo-v2.5
ai.pool.providers[1].weight=60
ai.pool.providers[1].enabled=true

ai.pool.vision-provider=mimo2

# ===== 服务端口 =====
server.port=8080
```

### 6.3 配置 CORS 白名单

当前 CORS 已集中由 `SecurityConfig.java` 读取 `app.cors.*` 配置，不再维护单独的 `WebConfig.java`。部署时在 `application-prod.properties` 中加入服务器公网 IP 或域名：

```properties
app.cors.allowed-origin-patterns=http://localhost:*,http://127.0.0.1:*,http://你的服务器IP:*,https://你的域名:*
app.cors.allowed-methods=GET,POST,OPTIONS
app.cors.allowed-headers=Authorization,Content-Type,Accept,Origin,X-Requested-With,X-XSRF-TOKEN
app.cors.exposed-headers=Authorization
app.cors.allow-credentials=true
app.cors.max-age=3600
```

### 6.4 构建与运行

```bash
# 构建（跳过测试）
mvn clean package -DskipTests

# 使用生产配置运行
java -jar target/DreamArchive-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 或后台运行（推荐使用 systemd 管理）
nohup java -jar target/DreamArchive-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  > /var/log/dream-archive.log 2>&1 &
```

### 6.5 配置 Systemd 服务（推荐）

创建 `/etc/systemd/system/dream-archive.service`：

```ini
[Unit]
Description=DreamArchive Backend
After=network.target mysql.service redis.service minio.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/dream-archive
ExecStart=/usr/bin/java -jar target/DreamArchive-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
ExecStop=/bin/kill -SIGTERM $MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

# JVM 调优参数
Environment="JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC"

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl start dream-archive
systemctl enable dream-archive

# 查看日志
journalctl -u dream-archive -f
```

---

## 7. 前端部署

### 7.1 安装依赖

```bash
cd /opt/dream-archive/frontend
npm install
```

### 7.2 配置生产环境 API 地址

编辑 `frontend/src/api/axios.ts`，确保 `baseURL` 在生产环境指向正确的后端地址：

```typescript
const api = axios.create({
  baseURL: '',  // 生产环境由 Nginx 反向代理，保持空即可
  timeout: 10000
})
```

> **说明**：生产环境通过 Nginx 反向代理 `/api` 到后端，前端 `baseURL` 保持为空字符串即可。

### 7.3 构建

```bash
npm run build
```

构建产物输出到 `frontend/dist/` 目录。

### 7.4 部署静态文件

```bash
# 将构建产物复制到 Nginx 静态目录
cp -r /opt/dream-archive/frontend/dist/* /var/www/dream-archive/
```

---

## 8. Nginx 配置

### 8.1 安装 Nginx

```bash
yum install -y nginx
systemctl start nginx
systemctl enable nginx
```

### 8.2 配置站点

创建 `/etc/nginx/conf.d/dream-archive.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;  # 替换为你的域名或 IP

    # 前端静态资源
    root /var/www/dream-archive;
    index index.html;

    # Vue Router history 模式支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到后端
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 文件上传大小限制
        client_max_body_size 10m;

        # 超时设置（AI 解析可能较慢）
        proxy_read_timeout 120s;
        proxy_connect_timeout 10s;
        proxy_send_timeout 60s;
    }

    # Swagger UI（可选，生产环境建议关闭）
    # location /swagger-ui/ {
    #     proxy_pass http://127.0.0.1:8080/swagger-ui/;
    # }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript image/svg+xml;
    gzip_min_length 1024;
    gzip_comp_level 6;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

```bash
# 检查配置
nginx -t

# 重载配置
systemctl reload nginx
```

---

## 9. 生产环境配置

### 9.1 安全检查清单

- [ ] **数据库**：不使用 root 用户，设置强密码
- [ ] **Redis**：设置 `requirepass`，不暴露公网
- [ ] **MinIO**：设置强密钥，配置 Bucket 策略
- [ ] **CORS**：只允许实际使用的域名/IP
- [ ] **防火墙**：只开放必要端口（80/443），后端 8080 通过 Nginx 代理，不直接暴露
- [ ] **API 密钥**：`application-prod.properties` 不提交到 Git
- [ ] **日志**：配置日志轮转，避免磁盘打满

### 9.2 环境变量方式管理敏感配置（推荐）

不将密钥写入配置文件，改用环境变量：

```bash
# /etc/environment 或 systemd service 中设置
export DB_PASSWORD=YourStrongPassword123!
export REDIS_PASSWORD=YourRedisPassword
export MINIO_ACCESS_KEY=your-minio-access-key
export MINIO_SECRET_KEY=your-minio-secret-key
export MAIL_PASSWORD=your-smtp-authorization-code
export AI_API_KEY=your-api-key
```

在 `application-prod.properties` 中引用：

```properties
spring.datasource.password=${DB_PASSWORD}
spring.data.redis.password=${REDIS_PASSWORD}
minio.access-key=${MINIO_ACCESS_KEY}
minio.secret-key=${MINIO_SECRET_KEY}
spring.mail.password=${MAIL_PASSWORD}
```

### 9.3 JVM 调优

根据服务器内存调整 JVM 参数：

```bash
# 2GB 内存服务器
-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# 4GB 内存服务器
-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### 9.4 数据库备份

```bash
# 创建备份脚本 /opt/scripts/backup-db.sh
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/data/backups/mysql"
mkdir -p $BACKUP_DIR
mysqldump -u dream_app -p'YourStrongPassword123!' dream | gzip > $BACKUP_DIR/dream_${DATE}.sql.gz

# 保留最近 30 天备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

# 添加定时任务（每天凌晨 3 点）
# crontab -e
# 0 3 * * * /opt/scripts/backup-db.sh
```

---

## 10. HTTPS 配置（可选）

### 10.1 使用 Let's Encrypt 免费证书

```bash
# 安装 Certbot
yum install -y certbot python3-certbot-nginx

# 申请证书
certbot --nginx -d your-domain.com

# 自动续期
echo "0 2 * * 1 certbot renew --quiet" | crontab -
```

### 10.2 Nginx HTTPS 配置

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # ... 其余配置同上
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

---

## 11. 运维与监控

生产环境不能只依赖“服务能启动”和“接口能访问”。DreamArchive 至少需要覆盖三类可观测能力：

- **监控**：持续观察应用、主机、数据库、Redis、MinIO、Nginx 和 AI provider 的健康状态。
- **告警**：当用户体验或数据安全已经受影响，或即将受影响时主动通知维护者。
- **日志集中化**：将后端、Nginx 和基础服务日志汇聚到统一位置，便于按时间、用户、接口、错误类型和 AI provider 排查问题。

### 11.1 常用运维命令

```bash
# 后端服务管理
systemctl start dream-archive
systemctl stop dream-archive
systemctl restart dream-archive
systemctl status dream-archive
journalctl -u dream-archive -f          # 实时日志
journalctl -u dream-archive --since today # 今日日志

# Nginx 管理
systemctl reload nginx
nginx -t                                 # 检查配置

# MinIO 管理
systemctl status minio

# Redis 管理
systemctl status redis
redis-cli -a YourRedisPassword info

# MySQL 管理
systemctl status mysqld
mysqladmin -u root -p status
```

### 11.2 监控指标设计

#### 应用层指标

| 指标 | 说明 | 建议阈值 |
|------|------|----------|
| `/api/hello` 可用性 | 后端基础健康检查 | 连续 3 次失败告警 |
| API 5xx 错误率 | 后端异常比例 | 5 分钟内 > 2% 告警 |
| API P95 延迟 | 用户请求响应时间 | 普通接口 > 1s，AI 提交接口 > 5s 关注 |
| 登录/注册失败率 | 账号体系是否异常 | 10 分钟内明显高于日常基线告警 |
| 邮件验证码发送失败数 | SMTP 或配置异常 | 5 分钟内连续失败告警 |
| 图片上传失败数 | MinIO、文件校验或网络异常 | 5 分钟内连续失败告警 |
| 异步任务失败数 | AI 解析、标题生成等后台任务 | 5 分钟内连续失败告警 |

#### AI 资源池指标

AI 调用是最容易受外部服务影响的链路，需要单独观察：

| 指标 | 说明 | 建议阈值 |
|------|------|----------|
| provider 可用数量 | 启用且未熔断的 provider 数量 | 可用数量 = 0 立即告警 |
| provider 连续失败次数 | `AiProvider.failCount` | 任一 provider 达到 3 次告警 |
| provider 熔断状态 | `AiProvider.circuitOpen` | 熔断打开告警或至少记录事件 |
| provider 平均延迟 | `AiProvider.avgLatencyMs` | 连续 10 分钟 > 5s 告警 |
| AI 请求非 2xx 数量 | 模型接口错误、限流、认证失败 | 5 分钟内连续失败告警 |
| AI 解析失败率 | `analysis_status=FAILED` 占比 | 15 分钟内 > 10% 告警 |

当前管理员后台已能查看 provider 状态，可作为人工巡检入口。后续接入 Prometheus 时，可将这些状态暴露为 metrics，直接进入 Grafana 面板和 Alertmanager 告警。

#### 基础设施指标

| 组件 | 关键指标 | 建议阈值 |
|------|----------|----------|
| 主机 | CPU、内存、磁盘使用率、磁盘 inode、网络流量 | 磁盘 > 80% 预警，> 90% 告警 |
| JVM | 堆内存、GC 次数和耗时、线程数、进程存活 | Full GC 频繁或进程退出告警 |
| MySQL | 连接数、慢查询、锁等待、磁盘空间、备份结果 | 连接接近上限或备份失败告警 |
| Redis | `PING`、内存、连接数、key 过期策略、持久化状态 | Redis 不可达立即告警 |
| MinIO | `/minio/health/live`、磁盘容量、请求错误率 | 不可达或容量 > 80% 告警 |
| Nginx | 4xx/5xx、访问延迟、上游连接失败 | 5xx 持续升高告警 |

### 11.3 推荐监控架构

小规模单机部署可以先使用“轻量方案”，后续再升级为完整方案。

#### 轻量方案（最低可用）

- `systemd` 管理后端、Nginx、Redis、MySQL、MinIO，保证异常退出自动重启。
- `journalctl` + `logrotate` 保存和轮转本机日志。
- `crontab` 每 1 分钟访问 `/api/hello`、Redis `PING`、MinIO health，并将失败写入本机告警脚本。
- 告警通知可先使用邮件、企业微信机器人、钉钉机器人或 Server 酱。

适合个人项目、小流量展示环境，优点是部署简单；缺点是历史趋势、聚合查询和多维分析能力较弱。

#### 推荐生产方案

```text
Spring Boot / Nginx / MySQL / Redis / MinIO
        │
        ├─ Prometheus exporters / Actuator metrics
        │        │
        │        ├─ Prometheus：采集和存储时序指标
        │        ├─ Grafana：监控面板
        │        └─ Alertmanager：告警路由和通知
        │
        └─ Promtail / Filebeat
                 │
                 └─ Loki 或 Elasticsearch：集中日志检索
```

建议组件：

| 能力 | 推荐组件 | 说明 |
|------|----------|------|
| 应用指标 | Spring Boot Actuator + Micrometer Prometheus | 暴露 JVM、HTTP、线程池等指标 |
| 主机指标 | node_exporter | CPU、内存、磁盘、网络 |
| MySQL 指标 | mysqld_exporter | 连接数、慢查询、InnoDB 状态 |
| Redis 指标 | redis_exporter | 内存、命中率、连接数、持久化 |
| Nginx 指标 | nginx-prometheus-exporter | 连接、请求、状态码 |
| MinIO 指标 | MinIO Prometheus metrics | 存储容量、请求、错误 |
| 日志采集 | Promtail + Loki 或 Filebeat + Elasticsearch | Loki 更轻量，ELK 查询能力更强 |
| 告警通知 | Alertmanager | 邮件、Webhook、企业微信、钉钉 |

> 当前代码尚未接入 `spring-boot-starter-actuator` 和 `micrometer-registry-prometheus`。如果需要完整指标采集，后续应新增依赖，并只在内网或受保护路径暴露 `/actuator/prometheus`，不要直接暴露到公网。

### 11.4 告警规则建议

告警要避免过度噪声，优先覆盖影响用户、数据和成本的事件。

| 级别 | 场景 | 建议动作 |
|------|------|----------|
| P0 | 后端进程退出、`/api/hello` 连续失败、MySQL 不可用、Redis 不可用 | 立即通知，优先恢复服务 |
| P0 | 所有 AI provider 不可用或全部熔断 | 立即通知，临时关闭 AI 入口或切换备用 provider |
| P1 | API 5xx 错误率持续升高 | 查看应用日志、最近发布和外部依赖 |
| P1 | 磁盘空间 > 90%、数据库备份失败 | 清理日志/备份并确认备份链路 |
| P1 | 邮件验证码连续发送失败 | 检查 SMTP 授权码、服务商限制和网络 |
| P2 | 单个 AI provider 熔断、AI 平均延迟升高 | 降低权重、禁用异常 provider 或切换模型 |
| P2 | Nginx 4xx 突增 | 检查 CORS、认证失效、攻击流量或前端路径配置 |

建议告警收敛规则：

- 同一服务同一错误 10 分钟内只发送一次告警，恢复后发送恢复通知。
- 只有连续失败或持续超过阈值才告警，单次抖动只记录日志。
- 告警内容必须包含服务名、环境、时间、指标值、最近错误摘要和排查入口。

### 11.5 日志管理与集中化

```bash
# 配置 logrotate 防止日志膨胀
cat > /etc/logrotate.d/dream-archive << 'EOF'
/var/log/dream-archive.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
}
EOF
```

建议日志来源：

| 来源 | 位置 | 用途 |
|------|------|------|
| 后端应用 | `journalctl -u dream-archive` 或 `/var/log/dream-archive/app.log` | 业务异常、AI 调用、上传、验证码、认证失败 |
| Nginx access log | `/var/log/nginx/access.log` | 请求路径、状态码、耗时、来源 IP |
| Nginx error log | `/var/log/nginx/error.log` | 反代失败、静态资源错误、上游异常 |
| MySQL | MySQL error log / slow query log | 连接失败、慢查询、锁等待 |
| Redis | `/var/log/redis/redis.log` | 连接、持久化、内存淘汰 |
| MinIO | systemd journal 或 MinIO 日志 | 上传失败、磁盘容量、权限问题 |

集中化建议：

- **轻量方案**：Promtail 采集 systemd journal 和 Nginx 日志，写入 Loki，通过 Grafana 查询。
- **复杂检索方案**：Filebeat 采集日志，写入 Elasticsearch，通过 Kibana 查询。
- 后端日志中保留 `requestId`、用户 ID、接口路径、错误类型、AI provider 名称和耗时，避免记录 token、验证码、密码、API Key、Cookie、MinIO secret 等敏感信息。
- Nginx 建议增加 `$request_time`、`$upstream_response_time`、`$status`、`$request_id` 字段，便于定位慢请求和上游异常。
- ERROR 日志必须能定位堆栈；WARN 日志用于外部依赖降级、AI provider 熔断、验证码频率限制等可恢复问题。

### 11.6 健康检查与巡检

```bash
# 后端健康检查
curl http://localhost:8080/api/hello

# 前端可用性
curl -I http://your-domain.com/

# Redis 连通性
redis-cli -a YourRedisPassword ping

# MinIO 连通性
curl -I http://localhost:9000/minio/health/live
```

建议巡检频率：

| 频率 | 检查项 |
|------|--------|
| 每 1 分钟 | `/api/hello`、Nginx 首页、Redis `PING`、MinIO health |
| 每 5 分钟 | AI provider 可用数量、AI 平均延迟、5xx 错误率 |
| 每天 | MySQL 备份结果、磁盘容量、日志轮转结果 |
| 每周 | 慢查询、异常日志 Top N、AI provider 成本和失败分布 |

### 11.7 生产可观测性验收清单

上线前至少确认：

- [ ] 后端、Nginx、MySQL、Redis、MinIO 都有基础存活监控。
- [ ] `/api/hello` 连续失败会触发告警。
- [ ] 后端 5xx 错误率升高会触发告警。
- [ ] 磁盘空间超过阈值会触发告警，避免日志或备份打满磁盘。
- [ ] AI provider 全部不可用或全部熔断会触发告警。
- [ ] 邮件验证码连续发送失败会触发告警。
- [ ] 数据库备份失败会触发告警。
- [ ] 后端和 Nginx 日志可以按时间范围集中查询。
- [ ] 日志中不输出 token、密码、验证码、API Key、Cookie 等敏感信息。
- [ ] 告警通知至少接入一个可靠渠道，并测试过触发和恢复通知。

---

## 12. 常见问题

### Q1: 启动报错 `Communications link failure`（数据库连接失败）

- 检查 MySQL 是否启动：`systemctl status mysqld`
- 检查 MySQL 是否允许远程连接：`bind-address = 0.0.0.0`
- 检查防火墙是否放行 3306 端口
- 检查数据库用户名密码是否正确

### Q2: Redis 连接被拒绝

- 检查 Redis 配置中 `bind` 是否包含 `0.0.0.0`
- 检查是否设置了密码但连接时未提供
- 检查防火墙 6379 端口

### Q3: 前端请求后端返回 403

- CORS 白名单未包含前端访问的 IP/域名
- 检查 `application.properties` 或 `application-prod.properties` 中的 `app.cors.allowed-origin-patterns`
- 如果使用 HttpOnly Cookie + CSRF，确认 `app.cors.allowed-headers` 包含 `X-XSRF-TOKEN`
- 部署新服务器时必须将服务器 IP 或域名加入 CORS 白名单

### Q4: 图片上传失败

- 检查 MinIO 服务是否正常：`systemctl status minio`
- 检查 MinIO Bucket 是否创建且权限正确
- 检查 `minio.endpoint` 是否可达
- 检查文件大小是否超过 `spring.servlet.multipart.max-file-size`（5MB）

### Q5: 邮件发送失败

- QQ 邮箱 SMTP 需要使用授权码，不是 QQ 密码
- 在 QQ 邮箱 → 设置 → 账户 → POP3/SMTP 服务 → 开启并获取授权码
- 检查 `spring.mail.password` 是否为授权码
- 检查服务器是否放行 587 端口（出站）

### Q6: AI 解析无响应或超时

- 检查 AI Provider 的 API Key 是否有效
- 检查服务器是否能访问 `https://token-plan-cn.xiaomimimo.com`
- Nginx 的 `proxy_read_timeout` 建议设为 120s 以上
- 通过管理员后台 `/admin` → AI 资源池 查看 provider 状态和熔断情况

### Q7: 端口被占用

```bash
# 查看端口占用
netstat -tlnp | grep 8080
lsof -i :8080

# 杀掉占用进程
kill -9 <PID>
```

---

## 部署检查清单

部署完成后，逐项验证：

- [ ] `http://服务器IP` 能访问前端页面
- [ ] `http://服务器IP/api/hello` 返回健康检查响应
- [ ] 注册新用户，收到验证码邮件
- [ ] 登录后能正常记录梦境
- [ ] AI 解析功能正常返回
- [ ] 图片上传功能正常
- [ ] 管理员后台 `/admin` 可正常访问
- [ ] 深色模式切换正常
- [ ] 手机端访问布局正常
- [ ] 监控能覆盖后端、Nginx、MySQL、Redis、MinIO 和 AI provider
- [ ] 核心告警已配置并测试触发
- [ ] 后端和 Nginx 日志可集中查询，且不会泄露敏感信息
