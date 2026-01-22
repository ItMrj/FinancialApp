# FinancialApp 阿里云 ECS 部署指南

## 📋 目录

1. [准备阶段](#准备阶段)
2. [阿里云 ECS 购买与配置](#阿里云-ecs-购买与配置)
3. [服务器环境准备](#服务器环境准备)
4. [应用部署](#应用部署)
5. [SSL 证书配置](#ssl-证书配置)
6. [Nginx 配置](#nginx-配置)
7. [监控与日志](#监控与日志)
8. [安全加固](#安全加固)
9. [故障排查](#故障排查)

---

## 准备阶段

### 前置要求

- ✅ 阿里云账号
- ✅ 域名（可选，用于 HTTPS）
- ✅ 本地开发环境已配置完成
- ✅ 项目代码已推送到 Git 仓库

### 必要工具

- Git
- Docker
- Docker Compose
- SSH 客户端（如 PuTTY、XShell）

---

## 阿里云 ECS 购买与配置

### 1. 购买 ECS 实例

**推荐配置**：
```
规格：2核4GB 或 4核8GB
操作系统：Ubuntu 22.04 LTS / CentOS 7.9
带宽：按使用流量计费（起步 1Mbps）
存储：40GB SSD 系统盘 + 100GB 数据盘
```

**购买步骤**：
1. 登录阿里云控制台
2. 进入「云服务器 ECS」→「创建实例」
3. 选择付费模式（包年包月或按量付费）
4. 选择地域（建议靠近目标用户）
5. 选择实例规格（推荐 2核4GB）
6. 选择镜像（Ubuntu 22.04 LTS）
7. 选择存储（40GB SSD）
8. 配置安全组（见下文）
9. 设置登录凭证（推荐 SSH 密钥对）
10. 完成购买

### 2. 配置安全组规则

在 ECS 实例的安全组中添加以下规则：

| 协议 | 端口范围 | 授权对象 | 描述 |
|------|---------|---------|------|
| TCP | 22 | 0.0.0.0/0 | SSH 访问 |
| TCP | 80 | 0.0.0.0/0 | HTTP |
| TCP | 443 | 0.0.0.0/0 | HTTPS |
| TCP | 3306 | 172.17.0.0/16 | MySQL（仅 Docker 网络） |
| TCP | 6379 | 172.17.0.0/16 | Redis（仅 Docker 网络） |

**安全建议**：
- 限制 22 端口仅允许特定 IP 访问
- 阿里云内网使用时，关闭公网访问

---

## 服务器环境准备

### 1. 连接服务器

```bash
# 使用 SSH 密钥连接
ssh -i /path/to/your-key.pem root@your-ecs-public-ip

# 或使用密码连接
ssh root@your-ecs-public-ip
```

### 2. 安装 Docker

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
docker --version
```

### 3. 安装 Docker Compose

```bash
# 下载 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```

### 4. 配置防火墙（可选）

```bash
# 允许必要端口
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 启用防火墙
sudo ufw enable

# 查看状态
sudo ufw status
```

### 5. 创建项目目录

```bash
# 创建项目目录
mkdir -p /opt/financialapp
cd /opt/financialapp

# 创建必要的子目录
mkdir -p uploads logs/nginx nginx/ssl monitoring/prometheus monitoring/grafana/dashboards monitoring/grafana/datasources
```

---

## 应用部署

### 1. 上传项目文件

**方法一：使用 Git Clone（推荐）**

```bash
# 在服务器上克隆项目
cd /opt/financialapp
git clone https://your-git-repository-url.git .

# 或使用 SSH 密钥
git clone git@github.com:yourusername/FinancialApp.git .
```

**方法二：使用 SCP 上传**

```bash
# 在本地执行
scp -i /path/to/your-key.pem -r /path/to/FinancialApp/* root@your-ecs-public-ip:/opt/financialapp/
```

**方法三：使用 SFTP 工具**

- FileZilla
- WinSCP
- Cyberduck

### 2. 配置环境变量

```bash
# 复制环境变量示例文件
cp .env.example .env

# 编辑环境变量文件
nano .env
```

**关键配置项**：

```bash
# 数据库密码（必须修改）
MYSQL_PASSWORD=your-strong-password-here
MYSQL_ROOT_PASSWORD=your-root-password-here

# JWT 密钥（与 application.yml 保持一致）
JWT_SECRET=your-jwt-secret-here

# CORS 允许的域名
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# SSL 证书（后续配置）
# RSA 密钥保持不变
```

### 3. 修改 Nginx 配置

```bash
# 编辑 Nginx 配置文件
nano nginx/nginx.conf
```

**修改以下内容**：
- `server_name` 改为实际域名
- SSL 证书路径（稍后配置）
- 根据需要调整限流策略

### 4. 执行部署

```bash
# 给部署脚本添加执行权限
chmod +x deploy.sh

# 执行部署
./deploy.sh
```

**或者手动部署**：

```bash
# 使用生产环境配置
export COMPOSE_FILE=docker-compose.prod.yml

# 构建并启动服务
docker-compose -f docker-compose.prod.yml build
docker-compose -f docker-compose.prod.yml up -d

# 查看服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看应用日志
docker logs -f financial-app-prod
```

---

## SSL 证书配置

### 方案一：使用 Let's Encrypt（免费）

#### 1. 安装 Certbot

```bash
# 在宿主机上安装 Certbot
sudo apt install certbot python3-certbot-nginx -y
```

#### 2. 生成证书

```bash
# 确保域名已解析到服务器 IP
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com
```

#### 3. 复制证书到项目目录

```bash
# 复制证书
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem /opt/financialapp/nginx/ssl/
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem /opt/financialapp/nginx/ssl/
sudo cp /etc/letsencrypt/live/yourdomain.com/chain.pem /opt/financialapp/nginx/ssl/

# 修改权限
sudo chmod 644 /opt/financialapp/nginx/ssl/*
sudo chown root:root /opt/financialapp/nginx/ssl/*
```

#### 4. 配置自动续期

```bash
# 创建续期脚本
sudo nano /usr/local/bin/renew-ssl.sh
```

```bash
#!/bin/bash
# SSL 证书自动续期脚本

# 续期证书
sudo certbot renew --quiet

# 复制新证书到项目目录
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem /opt/financialapp/nginx/ssl/
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem /opt/financialapp/nginx/ssl/
sudo cp /etc/letsencrypt/live/yourdomain.com/chain.pem /opt/financialapp/nginx/ssl/

# 重启 Nginx
docker-compose -f /opt/financialapp/docker-compose.prod.yml restart nginx
```

```bash
# 添加执行权限
sudo chmod +x /usr/local/bin/renew-ssl.sh

# 添加定时任务（每月 1 号凌晨 3 点）
(crontab -l 2>/dev/null; echo "0 3 1 * * /usr/local/bin/renew-ssl.sh >> /var/log/ssl-renewal.log 2>&1") | crontab -
```

### 方案二：使用阿里云 SSL 证书（付费）

1. 登录阿里云控制台
2. 进入「SSL 证书」→「创建证书」
3. 购买免费 DV SSL 证书或付费证书
4. 下载证书（选择 Nginx 类型）
5. 上传到服务器：

```bash
# 上传证书文件到 nginx/ssl/ 目录
fullchain.pem
privkey.pem
chain.pem
```

---

## Nginx 配置

### 1. 修改配置文件

编辑 `nginx/nginx.conf`，主要修改：

```nginx
# 域名配置
server_name yourdomain.com www.yourdomain.com;

# SSL 证书路径（确保路径正确）
ssl_certificate /etc/nginx/ssl/fullchain.pem;
ssl_certificate_key /etc/nginx/ssl/privkey.pem;
ssl_trusted_certificate /etc/nginx/ssl/chain.pem;

# 安全头（根据需要调整）
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
```

### 2. 重启 Nginx

```bash
# 重新加载 Nginx 配置
docker-compose -f docker-compose.prod.yml exec nginx nginx -s reload

# 或重启容器
docker-compose -f docker-compose.prod.yml restart nginx
```

### 3. 验证配置

```bash
# 检查配置语法
docker-compose -f docker-compose.prod.yml exec nginx nginx -t

# 测试 SSL 证书
curl -I https://yourdomain.com
```

---

## 监控与日志

### 1. 查看应用日志

```bash
# 实时查看应用日志
docker logs -f financial-app-prod

# 查看最近 100 行
docker logs --tail 100 financial-app-prod

# 查看错误日志
docker logs financial-app-prod 2>&1 | grep ERROR
```

### 2. 查看 Nginx 日志

```bash
# 访问日志
tail -f logs/nginx/access.log

# 错误日志
tail -f logs/nginx/error.log
```

### 3. 配置日志轮转

创建 `/etc/logrotate.d/financialapp`：

```
/opt/financialapp/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 0644 appuser appuser
    sharedscripts
}
```

### 4. 启用 Prometheus 监控（可选）

```bash
# 启动监控服务
docker-compose -f docker-compose.prod.yml --profile monitoring up -d

# 访问 Prometheus
http://your-ecs-ip:9090

# 访问 Grafana
http://your-ecs-ip:3000
# 默认账号：admin / admin（首次登录需修改密码）
```

### 5. 健康检查

```bash
# 检查应用健康状态
curl http://localhost:8080/api/actuator/health

# 检查所有服务状态
docker-compose -f docker-compose.prod.yml ps
```

---

## 安全加固

### 1. 修改 SSH 配置

```bash
# 编辑 SSH 配置
sudo nano /etc/ssh/sshd_config

# 修改以下配置
PermitRootLogin no  # 禁止 root 登录
PasswordAuthentication no  # 禁用密码登录，只允许密钥登录
Port 22222  # 修改 SSH 端口（可选）

# 重启 SSH 服务
sudo systemctl restart sshd
```

### 2. 配置防火墙规则

```bash
# 只允许特定 IP 访问 22 端口
sudo ufw allow from your-ip-address to any port 22

# 或者修改端口后
sudo ufw allow 22222/tcp
```

### 3. 配置自动更新（Ubuntu）

```bash
# 安装 unattended-upgrades
sudo apt install unattended-upgrades -y

# 配置自动更新
sudo dpkg-reconfigure -plow unattended-upgrades
```

### 4. 定期备份数据

创建备份脚本 `/opt/backup.sh`：

```bash
#!/bin/bash
# 数据库备份脚本

BACKUP_DIR="/opt/backups"
DATE=$(date +%Y%m%d_%H%M%S)
MYSQL_CONTAINER="financial-mysql-prod"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份 MySQL
docker exec $MYSQL_CONTAINER mysqldump -u root -p${MYSQL_ROOT_PASSWORD} financial_app > $BACKUP_DIR/financial_app_${DATE}.sql

# 压缩备份文件
gzip $BACKUP_DIR/financial_app_${DATE}.sql

# 删除 30 天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

echo "Backup completed: financial_app_${DATE}.sql.gz"
```

添加定时任务：

```bash
(crontab -l 2>/dev/null; echo "0 2 * * * /opt/backup.sh >> /var/log/backup.log 2>&1") | crontab -
```

---

## 故障排查

### 1. 应用无法启动

```bash
# 查看应用日志
docker logs financial-app-prod

# 检查配置
docker exec financial-app-prod env | grep MYSQL

# 检查数据库连接
docker exec financial-mysql-prod mysql -u financial_user -p financial_app
```

### 2. 数据库连接失败

```bash
# 检查 MySQL 容器状态
docker ps | grep mysql

# 检查 MySQL 日志
docker logs financial-mysql-prod

# 测试数据库连接
docker exec financial-app-prod ping -c 3 mysql
```

### 3. Nginx 502 错误

```bash
# 检查 Nginx 配置
docker exec financial-nginx-prod nginx -t

# 检查应用健康状态
curl http://localhost:8080/api/actuator/health

# 查看 Nginx 错误日志
tail -f logs/nginx/error.log
```

### 4. SSL 证书问题

```bash
# 检查证书有效期
echo | openssl s_client -servername yourdomain.com -connect yourdomain.com:443 2>/dev/null | openssl x509 -noout -dates

# 检查证书文件权限
ls -la nginx/ssl/

# 查看证书内容
openssl x509 -in nginx/ssl/fullchain.pem -text -noout
```

### 5. 性能问题

```bash
# 查看容器资源占用
docker stats

# 查看 JVM 内存使用
docker exec financial-app-prod jmap -heap 1

# 查看线程情况
docker exec financial-app-prod jstack 1
```

---

## 常用命令速查

```bash
# ===== 服务管理 =====
docker-compose -f docker-compose.prod.yml up -d           # 启动所有服务
docker-compose -f docker-compose.prod.yml down           # 停止所有服务
docker-compose -f docker-compose.prod.yml restart        # 重启所有服务
docker-compose -f docker-compose.prod.yml ps             # 查看服务状态

# ===== 单个服务操作 =====
docker-compose -f docker-compose.prod.yml restart app    # 重启应用
docker-compose -f docker-compose.prod.yml restart nginx  # 重启 Nginx
docker-compose -f docker-compose.prod.yml logs -f app    # 查看应用日志

# ===== 进入容器 =====
docker exec -it financial-app-prod bash                  # 进入应用容器
docker exec -it financial-mysql-prod bash                # 进入 MySQL 容器
docker exec -it financial-redis-prod sh                  # 进入 Redis 容器

# ===== 数据库操作 =====
docker exec -it financial-mysql-prod mysql -u root -p    # 登录 MySQL
docker exec financial-mysql-prod mysqldump ...           # 备份数据库
docker exec financial-redis-prod redis-cli               # 登录 Redis

# ===== 清理 =====
docker system prune -a                                    # 清理所有未使用的资源
docker volume prune                                       # 清理未使用的卷
```

---

## 更新部署

当应用代码更新时，执行以下步骤：

```bash
# 1. 拉取最新代码
cd /opt/financialapp
git pull

# 2. 重新构建镜像
docker-compose -f docker-compose.prod.yml build app

# 3. 重启服务
docker-compose -f docker-compose.prod.yml up -d app

# 4. 查看日志确认启动成功
docker logs -f financial-app-prod
```

---

## 性能优化建议

### 1. JVM 参数调优

在 `docker-compose.prod.yml` 中调整：

```yaml
environment:
  JAVA_OPTS: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
```

### 2. MySQL 优化

创建 `mysql/conf.d/custom.cnf`：

```ini
[mysqld]
# 连接数配置
max_connections = 500
max_connect_errors = 1000

# 缓冲池大小
innodb_buffer_pool_size = 1G
innodb_buffer_pool_instances = 4

# 查询缓存
query_cache_size = 64M
query_cache_type = 1

# 日志配置
slow_query_log = 1
long_query_time = 2
slow_query_log_file = /var/log/mysql/slow-query.log
```

### 3. Redis 优化

在 `docker-compose.prod.yml` 中调整：

```yaml
redis:
  command: >
    redis-server
    --appendonly yes
    --maxmemory 256mb
    --maxmemory-policy allkeys-lru
```

---

## 联系支持

如有问题，请联系：
- 技术支持：support@financialapp.com
- GitHub Issues：https://github.com/yourusername/FinancialApp/issues

---

**最后更新**: 2024-01-16
**版本**: 1.0.0
