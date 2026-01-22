#!/bin/bash

################################################################################
# 快速部署脚本 - FinancialApp
# 用于阿里云 ECS 快速部署
################################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 项目配置
PROJECT_DIR="/opt/financialapp"
APP_NAME="financialapp"

# 打印带颜色的消息
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# 检查是否为 root 用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        print_message "$RED" "❌ 请使用 root 用户或 sudo 运行此脚本"
        exit 1
    fi
}

# 更新系统
update_system() {
    print_message "$BLUE" "📦 更新系统..."
    apt update && apt upgrade -y
    print_message "$GREEN" "✅ 系统更新完成"
}

# 安装 Docker
install_docker() {
    if command -v docker &> /dev/null; then
        print_message "$GREEN" "✅ Docker 已安装"
        return
    fi

    print_message "$BLUE" "🐳 安装 Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    rm get-docker.sh

    systemctl start docker
    systemctl enable docker

    print_message "$GREEN" "✅ Docker 安装完成"
}

# 安装 Docker Compose
install_docker_compose() {
    if command -v docker-compose &> /dev/null; then
        print_message "$GREEN" "✅ Docker Compose 已安装"
        return
    fi

    print_message "$BLUE" "🐳 安装 Docker Compose..."
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    print_message "$GREEN" "✅ Docker Compose 安装完成"
}

# 安装 Git
install_git() {
    if command -v git &> /dev/null; then
        print_message "$GREEN" "✅ Git 已安装"
        return
    fi

    print_message "$BLUE" "📦 安装 Git..."
    apt install -y git

    print_message "$GREEN" "✅ Git 安装完成"
}

# 安装 Certbot（用于 SSL 证书）
install_certbot() {
    if command -v certbot &> /dev/null; then
        print_message "$GREEN" "✅ Certbot 已安装"
        return
    fi

    print_message "$BLUE" "🔒 安装 Certbot..."
    apt install -y certbot python3-certbot-nginx

    print_message "$GREEN" "✅ Certbot 安装完成"
}

# 创建项目目录
create_project_dir() {
    print_message "$BLUE" "📁 创建项目目录..."

    mkdir -p $PROJECT_DIR
    mkdir -p $PROJECT_DIR/{uploads,logs/nginx,nginx/ssl,monitoring/{prometheus,grafana/dashboards,grafana/datasources},mysql/conf.d}

    print_message "$GREEN" "✅ 项目目录创建完成"
}

# 克隆项目
clone_project() {
    print_message "$BLUE" "📥 克隆项目..."

    if [ -d "$PROJECT_DIR/.git" ]; then
        print_message "$YELLOW" "⚠️  项目已存在，拉取最新代码..."
        cd $PROJECT_DIR
        git pull
    else
        read -p "请输入 Git 仓库地址: " GIT_REPO
        git clone $GIT_REPO $PROJECT_DIR
    fi

    print_message "$GREEN" "✅ 项目克隆完成"
}

# 配置环境变量
configure_env() {
    print_message "$BLUE" "⚙️  配置环境变量..."

    if [ ! -f "$PROJECT_DIR/.env" ]; then
        cp $PROJECT_DIR/.env.example $PROJECT_DIR/.env
        print_message "$YELLOW" "⚠️  请编辑 $PROJECT_DIR/.env 文件配置环境变量"
        read -p "按 Enter 继续或 Ctrl+C 退出..."
    fi

    print_message "$GREEN" "✅ 环境变量配置完成"
}

# 配置防火墙
configure_firewall() {
    print_message "$BLUE" "🔥 配置防火墙..."

    if command -v ufw &> /dev/null; then
        ufw allow 22/tcp
        ufw allow 80/tcp
        ufw allow 443/tcp
        ufw --force enable
        print_message "$GREEN" "✅ 防火墙配置完成"
    else
        print_message "$YELLOW" "⚠️  UFW 未安装，跳过防火墙配置"
    fi
}

# 生成 SSL 证书
generate_ssl_certificate() {
    read -p "是否生成 SSL 证书? (y/n): " GENERATE_SSL

    if [ "$GENERATE_SSL" != "y" ]; then
        print_message "$YELLOW" "⚠️  跳过 SSL 证书生成"
        return
    fi

    read -p "请输入域名 (如 example.com): " DOMAIN

    print_message "$BLUE" "🔒 生成 SSL 证书..."
    certbot certonly --standalone -d $DOMAIN -d www.$DOMAIN

    cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem $PROJECT_DIR/nginx/ssl/
    cp /etc/letsencrypt/live/$DOMAIN/privkey.pem $PROJECT_DIR/nginx/ssl/
    cp /etc/letsencrypt/live/$DOMAIN/chain.pem $PROJECT_DIR/nginx/ssl/

    chmod 644 $PROJECT_DIR/nginx/ssl/*

    print_message "$GREEN" "✅ SSL 证书生成完成"
}

# 部署应用
deploy_app() {
    print_message "$BLUE" "🚀 部署应用..."

    cd $PROJECT_DIR

    # 使用生产环境配置
    export COMPOSE_FILE=docker-compose.prod.yml

    # 构建镜像
    docker-compose -f $COMPOSE_FILE build app

    # 启动服务
    docker-compose -f $COMPOSE_FILE up -d

    # 等待服务启动
    sleep 10

    print_message "$GREEN" "✅ 应用部署完成"
}

# 健康检查
health_check() {
    print_message "$BLUE" "🏥 健康检查..."

    # 检查应用服务
    if curl -sf http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        print_message "$GREEN" "✅ 应用服务正常"
    else
        print_message "$RED" "❌ 应用服务异常"
        docker logs financial-app-prod --tail 50
        exit 1
    fi

    # 检查 Nginx
    if curl -sf http://localhost/ > /dev/null 2>&1; then
        print_message "$GREEN" "✅ Nginx 服务正常"
    else
        print_message "$RED" "❌ Nginx 服务异常"
        docker logs financial-nginx-prod --tail 50
        exit 1
    fi
}

# 显示部署信息
show_deployment_info() {
    print_message "$GREEN" "========================================"
    print_message "$GREEN"  "  🎉 部署成功！"
    print_message "$GREEN" "========================================"
    echo ""
    print_message "$YELLOW" "服务访问地址:"
    echo "  📡 API 地址:    http://$(curl -s ifconfig.me)/api"
    echo "  📊 健康检查:    http://$(curl -s ifconfig.me)/api/actuator/health"
    echo "  📝 Swagger:     http://$(curl -s ifconfig.me)/api/swagger-ui.html"
    echo "  🗄️  Adminer:     http://$(curl -s ifconfig.me):8081"
    echo ""
    print_message "$YELLOW" "常用命令:"
    echo "  查看日志:      docker logs -f financial-app-prod"
    echo "  查看状态:      docker-compose -f $PROJECT_DIR/docker-compose.prod.yml ps"
    echo "  重启服务:      docker-compose -f $PROJECT_DIR/docker-compose.prod.yml restart"
    echo "  停止服务:      docker-compose -f $PROJECT_DIR/docker-compose.prod.yml down"
    echo ""
    print_message "$YELLOW" "配置文件位置:"
    echo "  环境变量:      $PROJECT_DIR/.env"
    echo "  Nginx 配置:    $PROJECT_DIR/nginx/nginx.conf"
    echo "  应用日志:      $PROJECT_DIR/logs/"
    echo ""
    print_message "$GREEN" "========================================"
}

# 主函数
main() {
    print_message "$GREEN" "========================================"
    print_message "$GREEN" "  FinancialApp 快速部署脚本"
    print_message "$GREEN" "========================================"
    echo ""

    check_root
    update_system
    install_git
    install_docker
    install_docker_compose
    install_certbot
    create_project_dir
    clone_project
    configure_env
    configure_firewall
    generate_ssl_certificate
    deploy_app
    health_check
    show_deployment_info

    print_message "$GREEN" "✅ 部署完成！"
}

# 执行主函数
main
