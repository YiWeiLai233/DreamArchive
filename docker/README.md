# DreamArchive Docker 部署

## 文件说明

```
docker/
├── docker-compose.yml    # 服务编排 (MySQL + Redis + MinIO + 后端 + 前端)
├── Dockerfile            # 后端构建 (Maven → JRE 17)
├── frontend.Dockerfile   # 前端构建 (Node 18 → Nginx)
├── nginx.conf            # Nginx 配置 (反向代理 + 限流 + 缓存)
├── .env.example          # 环境变量模板
├── deploy.sh             # Linux 一键部署脚本
├── deploy.ps1            # Windows 一键部署脚本
└── README.md             # 本文件
```

## 快速部署

```bash
cd docker

# 1. 复制并编辑环境变量
cp .env.example .env
vim .env

# 2. 一键启动
chmod +x deploy.sh && ./deploy.sh

# 或手动
docker compose up -d
```

## 环境变量

| 变量 | 说明 |
|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `REDIS_PASSWORD` | Redis 访问密码 |
| `MINIO_ROOT_USER` | MinIO 管理员用户名 |
| `MINIO_ROOT_PASSWORD` | MinIO 管理员密码 |
| `MAIL_USERNAME` | QQ 邮箱地址 |
| `MAIL_PASSWORD` | QQ 邮箱 SMTP 授权码 |
| `AUTH_SECRET` | JWT 认证密钥（至少 32 位随机字符串） |
| `AI_API_KEY` | AI Provider API Key |

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost |
| 后端 API | http://localhost/api/hello |
| MinIO 控制台 | http://localhost:9001 (仅本机访问) |

## 本地开发 vs 生产部署

### 本地开发

默认配置适合本地调试，如需访问数据库或 Redis，可在 `docker-compose.yml` 中临时开放端口：

```yaml
mysql:
  ports:
    - "3306:3306"

redis:
  ports:
    - "6379:6379"

backend:
  ports:
    - "8080:8080"
```

Swagger UI 默认注释掉，本地开发时取消 `docker/nginx.conf` 中的注释即可。

### 生产部署

生产环境需注意：

- **端口**：只暴露 80/443，MySQL、Redis、MinIO API、后端 8080 不暴露公网
- **Redis**：必须设置强密码（`REDIS_PASSWORD`）
- **AUTH_SECRET**：使用 32 位以上随机字符串，部署脚本会校验长度
- **HTTPS**：在 nginx 前加一层反代或挂载证书，设置 `app.auth.cookie-secure=true`
- **Swagger**：保持注释状态，避免暴露接口文档
- **MinIO 控制台**：默认绑定 `127.0.0.1:9001`，仅本机可访问

## 常用命令

```bash
docker compose ps              # 查看状态
docker compose logs -f         # 查看所有日志
docker compose logs -f backend # 查看后端日志
docker compose down            # 停止服务
docker compose up -d --build   # 重建并启动
docker compose down -v         # 停止并删除数据卷（慎用）
```

## 故障排查

### 后端启动失败

```bash
# 查看后端日志
docker compose logs backend

# 常见原因：
# 1. MySQL 未就绪 — 等待 healthcheck 通过
# 2. 数据库密码错误 — 检查 .env 中 MYSQL_ROOT_PASSWORD
# 3. Redis 连接失败 — 检查 REDIS_PASSWORD 是否一致
```

### 前端白屏

```bash
# 查看 Nginx 日志
docker compose logs frontend

# 常见原因：
# 1. 构建失败 — 检查 npm run build 输出
# 2. API 代理异常 — 确认 backend 容器健康
```

### 图片上传失败

```bash
# 检查 MinIO 是否正常
docker compose logs minio

# 检查 bucket 是否创建
docker compose logs minio-init

# 常见原因：
# 1. MinIO 未启动完成 — minio-init 会在 MinIO 健康后自动执行
# 2. 存储空间不足 — 检查 df -h
```

### 重建单个服务

```bash
docker compose up -d --build backend   # 只重建后端
docker compose up -d --build frontend  # 只重建前端
```
