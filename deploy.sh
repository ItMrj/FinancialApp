#!/bin/bash

################################################################################
# FinancialApp 阿里云 ECS 部署脚本
################################################################################

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目配置
APP_NAME="financial-app"
VERSION="1.0.0"
JAR_NAME="financial-app-${VERSION}.jar"
DOCKER_IMAGE="${APP_NAME}:${VERSION}"
DOCKER_CONTAINER="${APP_NAME}"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  FinancialApp 部署脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

################################################################################
# 1. 环境检查
################################################################################
echo -e "${YELLOW}[1/7] 检查环境依赖...${NC}"

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker 已安装${NC}"

# 检查 Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose 未安装，请先安装 Docker Compose${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker Compose 已安装${NC}"

# 检查环境变量文件
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  未找到 .env 文件，从示例文件复制...${NC}"
    if [ -f .env.example ]; then
        cp .env.example .env
        echo -e "${YELLOW}⚠️  请先编辑 .env 文件配置环境变量${NC}"
        exit 1
    else
        echo -e "${RED}❌ 未找到 .env.example 文件${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✅ 环境变量文件已加载${NC}"

################################################################################
# 2. 加载环境变量
################################################################################
echo -e "${YELLOW}[2/7] 加载环境变量...${NC}"
export $(cat .env | grep -v '^#' | xargs)
echo -e "${GREEN}✅ 环境变量加载完成${NC}"

################################################################################
# 3. 停止旧容器
################################################################################
echo -e "${YELLOW}[3/7] 停止旧容器...${NC}"

# 停止并删除旧容器
if docker ps -a --format '{{.Names}}' | grep -q "^${DOCKER_CONTAINER}$"; then
    echo -e "${YELLOW}停止并删除旧容器: ${DOCKER_CONTAINER}${NC}"
    docker stop ${DOCKER_CONTAINER} 2>/dev/null || true
    docker rm ${DOCKER_CONTAINER} 2>/dev/null || true
    echo -e "${GREEN}✅ 旧容器已清理${NC}"
else
    echo -e "${GREEN}✅ 无旧容器需要清理${NC}"
fi

################################################################################
# 4. 构建新镜像
################################################################################
echo -e "${YELLOW}[4/7] 构建 Docker 镜像...${NC}"
docker-compose build app

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Docker 镜像构建成功${NC}"
else
    echo -e "${RED}❌ Docker 镜像构建失败${NC}"
    exit 1
fi

################################################################################
# 5. 启动服务
################################################################################
echo -e "${YELLOW}[5/7] 启动服务...${NC}"

# 启动所有服务
docker-compose up -d

# 等待服务启动
echo -e "${YELLOW}等待服务启动...${NC}"
sleep 10

################################################################################
# 6. 健康检查
################################################################################
echo -e "${YELLOW}[6/7] 健康检查...${NC}"

# 检查 MySQL
echo -e "${YELLOW}检查 MySQL 服务...${NC}"
if docker exec financial-mysql mysqladmin ping -h localhost --silent; then
    echo -e "${GREEN}✅ MySQL 服务正常${NC}"
else
    echo -e "${RED}❌ MySQL 服务异常${NC}"
    exit 1
fi

# 检查 Redis
echo -e "${YELLOW}检查 Redis 服务...${NC}"
if docker exec financial-redis redis-cli ping | grep -q PONG; then
    echo -e "${GREEN}✅ Redis 服务正常${NC}"
else
    echo -e "${RED}❌ Redis 服务异常${NC}"
    exit 1
fi

# 检查应用服务
echo -e "${YELLOW}检查应用服务...${NC}"
HEALTH_CHECK_URL="http://localhost:8080/api/actuator/health"
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -sf $HEALTH_CHECK_URL > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 应用服务正常${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -e "${YELLOW}等待应用启动... ($RETRY_COUNT/$MAX_RETRIES)${NC}"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}❌ 应用服务启动超时${NC}"
    echo -e "${YELLOW}查看应用日志:${NC}"
    docker logs ${DOCKER_CONTAINER} --tail 50
    exit 1
fi

################################################################################
# 7. 显示部署信息
################################################################################
echo -e "${YELLOW}[7/7] 显示部署信息...${NC}"
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  🎉 部署成功！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}服务访问地址:${NC}"
echo -e "  📡 API 地址:    http://localhost:8080/api"
echo -e "  📊 健康检查:    http://localhost:8080/api/actuator/health"
echo -e "  📈 Prometheus:  http://localhost:8080/api/actuator/prometheus"
echo -e "  📝 Swagger UI:  http://localhost:8080/api/swagger-ui.html (如已启用)"
echo -e "  🗄️  Adminer:    http://localhost:8081"
echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo -e "  查看日志:      docker logs -f ${DOCKER_CONTAINER}"
echo -e "  停止服务:      docker-compose down"
echo -e "  重启服务:      docker-compose restart"
echo -e "  查看状态:      docker-compose ps"
echo ""
echo -e "${GREEN}========================================${NC}"
