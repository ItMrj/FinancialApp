# FinancialApp 阿里云 ECS 部署快速参考

## 📁 新增文件清单

### 配置文件
- ✅ `src/main/resources/application-prod.yml` - 生产环境配置
- ✅ `.env.example` - 环境变量示例
- ✅ `docker-compose.prod.yml` - 生产环境 Docker Compose 配置
- ✅ `Dockerfile.prod` - 优化后的生产环境 Dockerfile
- ✅ `nginx/nginx.conf` - Nginx 反向代理配置

### 部署脚本
- ✅ `deploy.sh` - 标准部署脚本
- ✅ `scripts/quick-deploy.sh` - 快速部署脚本（Linux）
- ✅ `scripts/quick-deploy.ps1` - 部署准备脚本（Windows）

### 文档
- ✅ `DEPLOYMENT_GUIDE.md` - 详细部署指南
- ✅ `DEPLOYMENT_QUICK_REFERENCE.md` - 快速参考（本文件）

### 监控配置
- ✅ `monitoring/prometheus.yml` - Prometheus 配置

---

## 🚀 快速部署流程

### 方案一：使用快速部署脚本（推荐）

#### Windows 端准备
```powershell
# 1. 在项目根目录执行
cd "G:\My Service\FinancialApp"

# 2. 执行准备脚本
.\scripts\quick-deploy.ps1

# 3. 将生成的 deploy-xxxxx.zip 上传到服务器
```

#### 服务器端部署
```bash
# 1. 解压部署包
unzip deploy-20240116_143000.zip
cd deploy-20240116_143000

# 2. 配置环境变量
nano .env
# 修改数据库密码、JWT 密钥、CORS 域名等

# 3. 执行快速部署
chmod +x quick-deploy.sh
sudo ./quick-deploy.sh
```

### 方案二：手动部署

```bash
# 1. 安装 Docker 和 Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 2. 创建项目目录
mkdir -p /opt/financialapp
cd /opt/financialapp

# 3. 克隆项目或上传文件
git clone https://your-repo-url.git .

# 4. 配置环境变量
cp .env.example .env
nano .env  # 编辑配置

# 5. 生成 SSL 证书（可选）
sudo apt install certbot python3-certbot-nginx -y
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/yourdomain.com/chain.pem nginx/ssl/

# 6. 修改 Nginx 配置
nano nginx/nginx.conf
# 修改 server_name 为实际域名

# 7. 执行部署
chmod +x deploy.sh
./deploy.sh
```

---

## ⚙️ 关键配置项

### .env 环境变量

```bash
# 数据库配置（必须修改）
MYSQL_PASSWORD=your-strong-password-here
MYSQL_ROOT_PASSWORD=your-root-password-here

# JWT 密钥（与 application.yml 保持一致）
JWT_SECRET=Fk9mP2vQ7xJ4nR8cW3dY6gB1hV5sE9tL0zM4uC7kA2iO5pS8jT1zN6wX3qD7bG4vH0rY5fK2nU8eP6aC9lI3sZ1oE5wM7tR0jV4bH8xQ2cN6yF9kG3pL7dS0iW4zE5uM9tR8jV2cX6yN3bP5kG0qL8dS1iW4zE5uM9tR8jV2cX6yN3bP5kG0qL8dS1i

# CORS 域名（必须修改）
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Swagger（生产环境建议关闭）
SWAGGER_ENABLED=false
```

### Nginx 配置

```nginx
# 修改 nginx/nginx.conf 中的以下内容
server_name yourdomain.com www.yourdomain.com;

# SSL 证书路径（如使用 Let's Encrypt）
ssl_certificate /etc/nginx/ssl/fullchain.pem;
ssl_certificate_key /etc/nginx/ssl/privkey.pem;
ssl_trusted_certificate /etc/nginx/ssl/chain.pem;
```

---

## 🔍 健康检查

```bash
# 检查应用健康状态
curl http://localhost:8080/api/actuator/health

# 检查所有服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看应用日志
docker logs -f financial-app-prod

# 查看 Nginx 日志
tail -f logs/nginx/error.log
```

---

## 📊 服务访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| API | `http://yourdomain.com/api` | 主要 API 接口 |
| 健康检查 | `http://yourdomain.com/api/actuator/health` | 应用健康状态 |
| Prometheus | `http://yourdomain.com:9090` | 监控指标 |
| Grafana | `http://yourdomain.com:3000` | 可视化仪表板 |
| Adminer | `http://yourdomain.com:8081` | 数据库管理 |

---

## 🛠️ 常用命令

```bash
# 服务管理
docker-compose -f docker-compose.prod.yml up -d      # 启动服务
docker-compose -f docker-compose.prod.yml down      # 停止服务
docker-compose -f docker-compose.prod.yml restart   # 重启服务
docker-compose -f docker-compose.prod.yml ps        # 查看状态

# 日志查看
docker logs -f financial-app-prod                   # 应用日志
docker logs -f financial-nginx-prod                 # Nginx 日志
docker logs -f financial-mysql-prod                 # MySQL 日志

# 进入容器
docker exec -it financial-app-prod bash              # 应用容器
docker exec -it financial-mysql-prod bash           # MySQL 容器
docker exec -it financial-redis-prod sh              # Redis 容器

# 数据库操作
docker exec -it financial-mysql-prod mysql -u root -p
docker exec financial-mysql-prod mysqldump -u root -p financial_app > backup.sql

# Redis 操作
docker exec -it financial-redis-prod redis-cli
```

---

## 🔄 更新部署

```bash
# 1. 拉取最新代码
cd /opt/financialapp
git pull

# 2. 重新构建镜像
docker-compose -f docker-compose.prod.yml build app

# 3. 重启服务
docker-compose -f docker-compose.prod.yml up -d app

# 4. 查看日志
docker logs -f financial-app-prod
```

---

## 🔒 安全加固

```bash
# 1. 修改 SSH 配置
sudo nano /etc/ssh/sshd_config
PermitRootLogin no
PasswordAuthentication no

# 2. 配置防火墙
sudo ufw allow from your-ip to any port 22
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# 3. 定期更新系统
sudo apt update && sudo apt upgrade -y
```

---

## 💾 数据备份

```bash
# 创建备份脚本
cat > /opt/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
docker exec financial-mysql-prod mysqldump -u root -p${MYSQL_ROOT_PASSWORD} financial_app > $BACKUP_DIR/financial_app_${DATE}.sql
gzip $BACKUP_DIR/financial_app_${DATE}.sql
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete
EOF

chmod +x /opt/backup.sh

# 添加定时任务
crontab -e
# 添加：0 2 * * * /opt/backup.sh >> /var/log/backup.log 2>&1
```

---

## 🐛 故障排查

### 应用无法启动
```bash
docker logs financial-app-prod
docker exec financial-app-prod env | grep MYSQL
```

### 数据库连接失败
```bash
docker ps | grep mysql
docker logs financial-mysql-prod
docker exec financial-app-prod ping mysql
```

### Nginx 502 错误
```bash
docker exec financial-nginx-prod nginx -t
curl http://localhost:8080/api/actuator/health
tail -f logs/nginx/error.log
```

### SSL 证书问题
```bash
openssl x509 -in nginx/ssl/fullchain.pem -text -noout
ls -la nginx/ssl/
```

---

## 📚 相关文档

- **详细部署指南**: `DEPLOYMENT_GUIDE.md`
- **项目 README**: `README.md`
- **开发规范**: `DEVELOPMENT_GUIDE.md`

---

## 🆘 获取帮助

如遇到问题：
1. 查看详细部署指南：`DEPLOYMENT_GUIDE.md`
2. 检查应用日志：`docker logs -f financial-app-prod`
3. 查看项目 Issues：https://github.com/yourusername/FinancialApp/issues
4. 联系技术支持：support@financialapp.com

---

**最后更新**: 2024-01-16
**版本**: 1.0.0
