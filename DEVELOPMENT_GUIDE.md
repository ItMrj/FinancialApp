# FinancialApp 开发指南

本指南整合了项目的完整开发、部署、测试和故障排除信息。

## 📚 目录

- [快速开始](#快速开始)
- [Docker 部署](#docker-部署)
- [API 接口文档](#api-接口文档)
- [RSA 密码加密](#rsa-密码加密)
- [请求日志功能](#请求日志功能)
- [数据库操作](#数据库操作)
- [故障排除](#故障排除)
- [开发工具](#开发工具)

---

## 快速开始

### 前置要求

- JDK 17+
- Gradle 8.x
- Docker Desktop（推荐）
- MySQL 8.0+ / Docker
- Redis 6.0+ / Docker

### 快速启动

#### 方式1：使用 Docker Compose（推荐）

```bash
# 启动数据库服务
docker compose up -d mysql redis adminer

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
```

#### 方式2：启动 Spring Boot 应用

```bash
# Windows
gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

#### 方式3：打包运行

```bash
# 打包
gradlew.bat clean build

# 运行
java -jar build/libs/FinancialApp-0.0.1-SNAPSHOT.jar
```

### 访问应用

- **API 文档**: http://localhost:8080/api/swagger-ui.html
- **API 基础URL**: http://localhost:8080/api
- **Adminer 数据库管理**: http://localhost:8081
  - 系统: MySQL
  - 服务器: `mysql`
  - 用户名: `root` / 密码: `root`

### 默认管理员账户

- 用户名: `admin`
- 密码: `admin123`
- 邮箱: `admin@financialapp.com`

⚠️ 警告：这是测试账户，生产环境请修改密码或删除

---

## Docker 部署

### 配置镜像加速（中国大陆用户）

编辑 Docker Desktop → Settings → Docker Engine：

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.aliyun.com",
    "https://mirror.ccs.tencentyun.com"
  ]
}
```

点击 **Apply & Restart**

### 使用阿里云镜像源

如果官方镜像源不可用，使用 `docker-compose-aliyun.yml`：

```bash
docker compose -f docker-compose-aliyun.yml up -d
```

### 常用 Docker 命令

```bash
# 查看容器状态
docker compose ps

# 查看日志
docker compose logs -f app

# 重启服务
docker compose restart

# 停止所有服务
docker compose down

# 停止并删除数据卷
docker compose down -v

# 进入容器
docker exec -it financial-mysql bash
docker exec -it financial-redis redis-cli
```

### 数据库连接

- 主机: `localhost`
- 端口: `3306`
- 数据库: `financial_app`
- 用户名: `root`
- 密码: `root`

---

## API 接口文档

### 基础信息

- **基础URL**: `http://localhost:8080/api`
- **认证方式**: JWT Bearer Token
- **响应格式**: JSON
- **字符编码**: UTF-8

### 统一响应格式

**成功响应**
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    // 业务数据
  }
}
```

**失败响应**
```json
{
  "success": false,
  "message": "错误信息",
  "data": null
}
```

### 认证接口

#### 1. 用户注册

```
POST /api/auth/register
```

**请求示例**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "confirmPassword": "password123",
    "email": "test@example.com",
    "firstName": "张",
    "lastName": "三"
  }'
```

#### 2. 用户登录

```
POST /api/auth/login
```

**请求示例**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

#### 3. 获取当前用户信息

```
GET /api/auth/me
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**请求示例**
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 4. 用户登出

```
POST /api/auth/logout
Authorization: Bearer YOUR_ACCESS_TOKEN
```

#### 5. 刷新Token

```
POST /api/auth/refresh
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### 用户接口

#### 1. 获取当前用户信息

```
GET /api/users/me
Authorization: Bearer YOUR_ACCESS_TOKEN
```

#### 2. 根据ID获取用户信息

```
GET /api/users/{id}
Authorization: Bearer YOUR_ACCESS_TOKEN
权限: ROLE_ADMIN
```

### 文件上传接口

#### 1. 上传头像

```
POST /api/users/avatar
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: multipart/form-data
```

**请求示例 (curl)**
```bash
curl -X POST http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "avatar=@/path/to/your/avatar.jpg"
```

**请求示例 (JavaScript)**
```javascript
const formData = new FormData();
formData.append('avatar', fileInput.files[0]);

fetch('http://localhost:8080/api/users/avatar', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  body: formData
});
```

#### 2. 删除头像

```
DELETE /api/users/avatar
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### RSA 加密接口

#### 获取公钥

```
GET /api/rsa/public-key
无需认证
```

**响应示例**
```json
{
  "success": true,
  "message": "获取公钥成功",
  "data": {
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
    "algorithm": "RSA",
    "keySize": "2048"
  }
}
```

**使用步骤**

1. 调用 `/api/rsa/public-key` 获取公钥
2. 使用公钥加密密码（JSEncrypt）
3. 在登录/注册请求中添加 `encrypted: true` 标识

### 错误码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或Token无效 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## RSA 密码加密

### 功能概述

系统已实现完整的RSA密码加解密功能，确保密码在传输过程中的安全性。

### 核心特性

- ✅ **RSA-2048加密**：使用2048位RSA密钥对进行加密
- ✅ **BCrypt存储**：解密后的密码使用BCrypt哈希存储
- ✅ **传输加密**：密码在传输过程中始终加密
- ✅ **向后兼容**：支持明文密码模式（仅用于开发/测试）

### 生成密钥

#### 方式1：自动生成（首次启动）

应用启动时会自动生成临时密钥对。

#### 方式2：手动生成密钥

```bash
# 使用 OpenSSL 生成密钥对
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```

### 配置密钥

编辑 `src/main/resources/application.yml`：

```yaml
rsa:
  public-key: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
  private-key: "MIIEvgIBADANBgkqhkiG9w0BAQEFAASC..."
```

### 前端集成示例

```html
<script src="https://cdnjs.cloudflare.com/ajax/libs/jsencrypt/3.3.2/jsencrypt.min.js"></script>
<script>
// 1. 获取公钥
const response = await fetch('/api/rsa/public-key');
const data = await response.json();
const publicKey = data.data.publicKey;

// 2. 加密密码
const encrypt = new JSEncrypt();
encrypt.setPublicKey(publicKey);
const encryptedPassword = encrypt.encrypt('MyPassword123');

// 3. 发送登录请求
await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'admin',
    password: encryptedPassword,
    encrypted: true
  })
});
</script>
```

### 安全注意事项

- ✅ 将RSA私钥存储在环境变量或密钥管理服务中
- ✅ 定期更换RSA密钥对（建议3-6个月）
- ❌ 不要将私钥提交到版本控制系统
- ✅ 生产环境始终使用HTTPS协议
- ✅ 强制要求 `encrypted: true`

---

## 请求日志功能

### 功能概述

`RequestLoggingFilter` 是一个标准的HTTP请求日志过滤器，用于记录所有进出应用的HTTP请求和响应的详细信息。

### 记录内容

- **请求信息**: HTTP方法、URL、查询字符串、客户端IP、User-Agent
- **请求头**: 所有HTTP请求头（敏感信息自动脱敏）
- **请求体**: JSON格式、Form Data、Multipart文件上传等
- **响应信息**: HTTP状态码、Content-Type、响应头、响应体
- **性能统计**: 每个请求的总耗时

### 安全特性

- **敏感信息脱敏**:
  - `Authorization` 显示为 `Bearer ***REDACTED***`
  - `Cookie` 显示为 `***REDACTED***`
  - `Set-Cookie` 显示为 `***REDACTED***`

- **路径排除**: 以下路径不会记录日志
  - `/health`
  - `/actuator/*`
  - `/swagger-ui/*`
  - `/api-docs/*`
  - `/webjars/*`

### 日志格式示例

```
========== 请求开始 ==========
请求方法: POST
请求URL: http://localhost:8080/api/auth/login
请求URI: /api/auth/login
查询字符串: 无
远程地址: 127.0.0.1
用户代理: PostmanRuntime/7.32.1
Content-Type: application/json
Content-Length: 58
查询参数: 无
请求头:
  accept: application/json
  content-type: application/json
  user-agent: PostmanRuntime/7.32.1
请求体 (JSON):
  {"username":"admin","password":"password123"}
==========================
---------- 响应信息 ----------
响应状态码: 200
Content-Type: application/json
Content-Length: 256
响应头:
  content-type: application/json
响应体 (JSON):
  {"token":"eyJhbGc...","type":"Bearer","id":1,"username":"admin"}
========== 请求结束 ==========
✅ 状态: 200 | 耗时: 50ms | POST /api/auth/login
==============================
```

### 配置日志级别

编辑 `src/main/resources/application.yml`：

```yaml
logging:
  level:
    com.financialapp.filter: DEBUG  # 显示详细日志
    com.financialapp.filter: INFO   # 显示基本日志
```

---

## 数据库操作

### 数据库连接信息

| 参数 | 值 |
|------|-----|
| 主机 | `localhost` |
| 端口 | `3306` |
| 数据库 | `financial_app` |
| 用户名 | `root` |
| 密码 | `root` |

### 连接到数据库

#### 方式1: 命令行

```bash
docker exec -it financial-mysql mysql -uroot -proot
```

#### 方式2: 图形化工具（DBeaver）

- 主机: `localhost`
- 端口: `3306`
- 数据库: `financial_app`
- 用户名: `root`
- 密码: `root`

### 表结构

#### users 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户ID（主键，自增） |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(255) | 密码（BCrypt加密） |
| email | VARCHAR(100) | 邮箱（唯一） |
| phone | VARCHAR(20) | 手机号（唯一） |
| first_name | VARCHAR(50) | 名 |
| last_name | VARCHAR(50) | 姓 |
| role | VARCHAR(20) | 角色（ADMIN/MANAGER/USER） |
| enabled | BOOLEAN | 是否启用 |
| status | VARCHAR(100) | 状态（ACTIVE/INACTIVE/LOCKED/PENDING） |
| avatar | VARCHAR(500) | 头像URL |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| last_login_at | DATETIME | 最后登录时间 |
| last_login_ip | VARCHAR(100) | 最后登录IP |

### 常用查询

```sql
-- 查看所有数据库
SHOW DATABASES;

-- 使用应用数据库
USE financial_app;

-- 查看所有表
SHOW TABLES;

-- 查看表结构
DESCRIBE users;

-- 查询所有用户
SELECT * FROM users;

-- 按用户名查询
SELECT * FROM users WHERE username = 'admin';

-- 查询所有管理员
SELECT * FROM users WHERE role = 'ADMIN';

-- 查询活跃用户
SELECT * FROM users WHERE status = 'ACTIVE' AND enabled = TRUE;

-- 统计用户总数
SELECT COUNT(*) AS total_users FROM users;

-- 按角色统计
SELECT role, COUNT(*) AS count FROM users GROUP BY role;
```

### 插入数据

```sql
INSERT INTO users (
    username, password, email, phone,
    first_name, last_name, role, enabled, status,
    avatar, created_at, updated_at
) VALUES (
    'testuser',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',
    'test@example.com',
    '13800138000',
    'Test',
    'User',
    'USER',
    TRUE,
    'ACTIVE',
    'default-avatar.png',
    NOW(),
    NOW()
);
```

### 更新数据

```sql
-- 更新用户信息
UPDATE users
SET first_name = 'John',
    last_name = 'Doe',
    updated_at = NOW()
WHERE username = 'testuser';

-- 更新用户状态
UPDATE users
SET status = 'INACTIVE',
    enabled = FALSE,
    updated_at = NOW()
WHERE id = 1;
```

### 删除数据

```sql
-- 删除单个用户
DELETE FROM users WHERE id = 1;

-- 清空表（谨慎使用）
TRUNCATE TABLE users;
```

### 备份与恢复

```bash
# 导出数据库
docker exec financial-mysql mysqldump -uroot -proot financial_app > backup.sql

# 导入数据库
docker exec -i financial-mysql mysql -uroot -proot financial_app < backup.sql

# 导出特定表
docker exec financial-mysql mysqldump -uroot -proot financial_app users > users_backup.sql
```

---

## 故障排除

### Gradle 相关问题

#### 问题：Unable to load class 'org.gradle.api.internal.HasConvention'

**解决方案**:

手动下载 gradle-wrapper.jar：
- 下载地址：https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar
- 保存到：`gradle\wrapper\gradle-wrapper.jar`

#### 问题：Gradle 构建失败，提示连接超时

**解决方案**:

编辑 `build.gradle.kts`，添加国内镜像源：

```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public/") }
    maven { url = uri("https://maven.aliyun.com/repository/spring/") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
    mavenCentral()
}
```

#### 问题：Java 版本不兼容

**解决方案**:

项目需要 Java 17，下载地址：https://adoptium.net/temurin/releases/?version=17

### 数据库相关问题

#### 问题：数据库连接失败

**解决方案**:

1. 检查 MySQL 服务是否启动
2. 检查数据库配置
3. 测试数据库连接
4. 创建数据库（如果不存在）

```sql
CREATE DATABASE financial_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 问题：表不存在错误

**解决方案**:

运行初始化脚本：
```bash
mysql -u root -p financial_app < database/init.sql
```

### Redis 相关问题

#### 问题：Redis 连接失败

**解决方案**:

1. 检查 Redis 服务是否启动
2. 测试 Redis 连接
3. 检查 Redis 配置

```bash
redis-cli ping
# 应该返回：PONG
```

### JWT 认证相关问题

#### 问题：Token 验证失败

**解决方案**:

1. 检查 Token 是否过期
2. 检查请求头格式：`Authorization: Bearer YOUR_ACCESS_TOKEN`（注意空格）
3. 检查 JWT 配置

### 端口占用问题

#### 问题：端口 8080 已被占用

**Windows 解决方案**:
```bash
# 查找占用端口的进程
netstat -ano | findstr :8080

# 结束进程
taskkill /PID <PID> /F
```

**Linux/Mac 解决方案**:
```bash
# 查找占用端口的进程
lsof -i :8080

# 结束进程
kill -9 <PID>
```

**修改端口**:
编辑 `src/main/resources/application.yml`:
```yaml
server:
  port: 8081
```

### Docker 相关问题

#### 问题：Docker 容器启动失败

**解决方案**:

```bash
# 检查 Docker 是否运行
docker ps

# 查看容器日志
docker compose logs

# 重新构建容器
docker compose down
docker compose build --no-cache
docker compose up -d

# 检查端口冲突
netstat -ano | findstr :8080
netstat -ano | findstr :3306
netstat -ano | findstr :6379
```

#### 问题：镜像源 DNS 解析失败

**解决方案**:

1. 更换镜像源：使用阿里云或腾讯云镜像源
2. 检查 DNS 设置
3. 使用代理/VPN
4. 手动拉取镜像

```bash
docker pull mysql:8.0
docker pull redis:7-alpine
```

---

## 开发工具

### 推荐工具

#### 数据库管理工具

1. **DBeaver**（推荐）
   - 免费开源
   - 支持多种数据库
   - 功能强大

2. **HeidiSQL**（Windows）
   - 轻量级
   - 界面友好

3. **MySQL Workbench**
   - 官方工具
   - 功能全面

#### API 测试工具

1. **Postman**（推荐）
   - 功能强大
   - 团队协作

2. **Swagger UI**
   - 内置在应用中
   - http://localhost:8080/api/swagger-ui.html

3. **curl**
   - 命令行工具
   - 快速测试

### 日志管理

#### 查看应用日志

```bash
# 实时查看日志
docker compose logs -f app

# 查看最近 100 行日志
docker compose logs --tail=100 app

# 查看特定时间的日志
docker compose logs --since="2024-01-15T10:00:00" app
```

#### 日志级别配置

编辑 `src/main/resources/application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.financialapp: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

### 性能监控

#### 请求耗时分析

通过请求日志功能，可以查看每个请求的耗时，识别慢请求。

#### 数据库查询监控

启用 Hibernate SQL 日志：
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## 项目架构

### 技术栈

- **后端框架**: Spring Boot 3.2.0
- **编程语言**: Kotlin 1.9.20
- **数据库**: MySQL 8.0+
- **ORM**: Spring Data JPA (Hibernate)
- **缓存**: Redis
- **安全框架**: Spring Security + JWT
- **密码加密**: RSA-2048 + BCrypt
- **API 文档**: SpringDoc OpenAPI (Swagger)
- **构建工具**: Gradle (Kotlin DSL)

### 项目结构

```
FinancialApp/
├── src/main/kotlin/com/financialapp/
│   ├── config/              # 配置层
│   │   ├── SecurityConfig      # 安全配置
│   │   ├── WebConfig          # Web配置
│   │   ├── JpaConfig          # JPA配置
│   │   └── RSAConfig          # RSA加密配置
│   ├── controller/           # 控制层
│   │   ├── AuthController     # 认证控制器
│   │   ├── UserController     # 用户控制器
│   │   └── RSAController      # RSA加密控制器
│   ├── service/              # 服务层
│   │   ├── AuthService        # 认证服务
│   │   ├── UserService        # 用户服务
│   │   └── RSAService         # RSA加密服务
│   ├── repository/           # 数据访问层
│   ├── entity/               # 实体层
│   ├── dto/                  # 数据传输对象
│   │   ├── request/          # 请求DTO
│   │   └── response/         # 响应DTO
│   ├── security/             # 安全组件
│   │   ├── JwtTokenProvider  # JWT令牌生成
│   │   ├── JwtAuthenticationFilter  # JWT认证过滤器
│   │   ├── UserDetailsImpl   # 用户详情实现
│   │   └── CustomUserDetailsService # 自定义用户详情服务
│   ├── filter/               # 过滤器
│   │   └── RequestLoggingFilter # HTTP请求日志过滤器
│   ├── exception/            # 异常处理
│   └── util/                 # 工具类
│       └── RSAUtil            # RSA加密工具类
├── src/main/resources/
│   └── application.yml       # 应用配置
├── database/                 # 数据库脚本
├── docs/                     # 项目文档
├── build.gradle.kts          # Gradle构建配置
├── settings.gradle.kts       # Gradle设置
└── Dockerfile                # Docker配置
```

### 核心功能

#### 1. 用户认证
- ✅ 用户登录（JWT Token 认证）
- ✅ 用户注册
- ✅ 用户登出
- ✅ Token 刷新
- ✅ 获取当前用户信息

#### 2. 用户管理
- ✅ 查询用户信息
- ✅ 用户状态管理
- ✅ 角色权限控制

#### 3. 安全特性
- ✅ **RSA密码加密** - 密码传输使用RSA-2048加密
- ✅ BCrypt 密码加密
- ✅ JWT Token 认证
- ✅ 基于角色的访问控制（RBAC）
- ✅ CORS 跨域配置
- ✅ 全局异常处理
- ✅ 请求参数验证
- ✅ HTTP请求日志记录

#### 4. 日志监控
- ✅ 请求/响应完整日志
- ✅ 敏感信息自动脱敏
- ✅ 请求耗时统计
- ✅ 错误状态高亮显示
- ✅ 支持路径排除过滤

### 用户角色

| 角色 | 说明 | 权限 |
|------|------|------|
| ADMIN | 管理员 | 所有权限 |
| MANAGER | 经理 | 部分管理权限 |
| USER | 普通用户 | 基本访问权限 |

### 用户状态

| 状态 | 说明 |
|------|------|
| ACTIVE | 活跃状态，可以正常使用 |
| INACTIVE | 非活跃状态，无法登录 |
| LOCKED | 锁定状态，无法登录 |
| PENDING | 待审核状态 |

---

## 测试

### 单元测试

```bash
# 运行所有测试
./gradlew test

# 运行指定测试类
./gradlew test --tests AuthServiceTest

# 生成测试报告
./gradlew test --report
```

### 集成测试

使用 Testcontainers 进行真实的数据库测试。

---

## 部署

### Docker 部署

```bash
# 构建镜像
docker build -t financial-app:latest .

# 运行容器
docker run -p 8080:8080 financial-app:latest
```

### 传统部署

```bash
# 打包
./gradlew clean build

# 运行
java -jar build/libs/FinancialApp-0.0.1-SNAPSHOT.jar
```

---

## 安全最佳实践

1. ✅ 始终使用 HTTPS 生产环境
2. ✅ 定期更新 JWT 密钥
3. ✅ 实现请求限流
4. ✅ 添加 CSRF 保护
5. ✅ 定期备份数据库
6. ✅ 使用环境变量管理敏感信息
7. ✅ 实施密钥轮换机制
8. ✅ 定期审查安全日志

---

## 联系方式

如有问题，请提交 Issue 或联系项目维护者。

---

## 许可证

MIT License

---

**注意**: 本项目仅用于学习和演示目的，生产环境使用请进行充分的安全加固和性能优化。
