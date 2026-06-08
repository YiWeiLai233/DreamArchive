#!/bin/bash
# DreamArchive 一键部署脚本
# 用法: cd docker && chmod +x deploy.sh && ./deploy.sh
set -e
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
cd "$(dirname "$0")"
echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   DreamArchive Docker 一键部署       ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
echo ""
# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: 未安装 Docker，请先安装 Docker${NC}"
    exit 1
fi
if ! docker compose version &> /dev/null; then
    echo -e "${RED}错误: 未安装 Docker Compose V2，请升级 Docker${NC}"
    exit 1
fi
# 检查 .env 文件
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo -e "${YELLOW}未检测到 .env 文件，正在从模板创建...${NC}"
        cp .env.example .env
        echo -e "${YELLOW}请编辑 .env 文件填写真实配置，然后重新运行此脚本${NC}"
        echo -e "${YELLOW}命令: vim .env${NC}"
        exit 1
    else
        echo -e "${RED}错误: 缺少 .env 和 .env.example 文件${NC}"
        exit 1
    fi
fi
# 检查必要环境变量
source .env
missing=0
for var in MYSQL_ROOT_PASSWORD REDIS_PASSWORD MINIO_ROOT_USER MINIO_ROOT_PASSWORD MAIL_USERNAME MAIL_PASSWORD AUTH_SECRET APP_ENCRYPTION_AES_KEY; do
    val=$(eval echo \$$var)
    if [ -z "$val" ] || [[ "$val" == "your_"* ]]; then
        echo -e "${RED}错误: .env 中 $var 未填写或仍为模板值${NC}"
        missing=1
    fi
done
if [ $missing -eq 1 ]; then
    echo -e "${YELLOW}请编辑 .env 文件填写真实配置后重新运行${NC}"
    exit 1
fi

# AUTH_SECRET 长度校验
if [ ${#AUTH_SECRET} -lt 32 ]; then
    echo -e "${RED}错误: AUTH_SECRET 至少需要 32 位，当前 ${#AUTH_SECRET} 位${NC}"
    exit 1
fi

# AES 密钥长度校验
if [ ${#APP_ENCRYPTION_AES_KEY} -ne 32 ]; then
    echo -e "${RED}错误: APP_ENCRYPTION_AES_KEY 必须恰好 32 位，当前 ${#APP_ENCRYPTION_AES_KEY} 位${NC}"
    exit 1
fi

echo -e "${GREEN}[1/3] 构建镜像...${NC}"
docker compose build --parallel

echo -e "${GREEN}[2/3] 启动服务...${NC}"
docker compose up -d

echo -e "${GREEN}[3/3] 等待服务就绪...${NC}"
echo "  MySQL  → 检查健康状态..."
docker compose exec -T mysql mysqladmin ping -h localhost -u root -p"${MYSQL_ROOT_PASSWORD}" --wait=30 > /dev/null 2>&1
echo "  Redis  → 检查健康状态..."
docker compose exec -T redis redis-cli -a "${REDIS_PASSWORD}" ping > /dev/null 2>&1
echo "  后端   → 等待启动 (约30-60秒)..."
timeout 120 bash -c 'until curl -sf http://localhost/api/hello > /dev/null 2>&1; do sleep 3; done'
echo "  前端   → 检查 Nginx..."
timeout 30 bash -c 'until curl -sf http://localhost > /dev/null 2>&1; do sleep 2; done'

echo ""
echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}║         部署完成！                    ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  前端:      http://localhost          ║${NC}"
echo -e "${GREEN}║  后端 API:  http://localhost/api/hello ║${NC}"
echo -e "${GREEN}║  MinIO:     http://localhost:9001      ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
echo ""
echo -e "常用命令:"
echo -e "  查看日志:  ${YELLOW}docker compose logs -f${NC}"
echo -e "  停止服务:  ${YELLOW}docker compose down${NC}"
echo -e "  重启后端:  ${YELLOW}docker compose restart backend${NC}"
echo -e "  查看状态:  ${YELLOW}docker compose ps${NC}"
