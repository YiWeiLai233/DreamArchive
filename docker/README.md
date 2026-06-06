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

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost |
| 后端 API | http://localhost/api/hello |
| MinIO 控制台 | http://localhost:9001 |
| Swagger | http://localhost/swagger-ui/ |

## 常用命令

```bash
docker compose ps              # 查看状态
docker compose logs -f         # 查看所有日志
docker compose logs -f backend # 查看后端日志
docker compose down            # 停止服务
docker compose up -d --build   # 重建并启动
```
