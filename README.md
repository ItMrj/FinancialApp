# FinancialApp - 金融应用后端项目

## 项目简介

基于 Spring Boot 3.x + Kotlin 构建的企业级金融应用后端系统，采用经典的分层架构设计，提供完善的用户认证和授权功能。

## 技术栈

- **后端框架**: Spring Boot 3.2.0
- **编程语言**: Kotlin 1.9.20
- **数据库**: MySQL 8.0+
- **ORM**: Spring Data JPA (Hibernate)
- **缓存**: Redis
- **安全框架**: Spring Security + JWT
- **密码加密**: RSA-2048 + BCrypt
- **前端加密**: JSEncrypt (JavaScript)
- **API 文档**: SpringDoc OpenAPI (Swagger)
- **构建工具**: Gradle (Kotlin DSL)

## 项目架构

```
FinancialApp/
├── config/              # 配置层
│   ├── SecurityConfig      # 安全配置
│   ├── WebConfig          # Web配置
│   ├── JpaConfig          # JPA配置
│   └── RSAConfig          # RSA加密配置
├── controller/           # 控制层 - 处理HTTP请求
│   ├── AuthController     # 认证控制器
│   ├── UserController     # 用户控制器
│   └── RSAController      # RSA加密控制器
├── service/              # 服务层 - 业务逻辑
│   ├── AuthService        # 认证服务
│   ├── UserService        # 用户服务
│   └── RSAService         # RSA加密服务
├── repository/           # 数据访问层 - 数据库操作
├── entity/               # 实体层 - 数据模型
├── dto/                  # 数据传输对象
│   ├── request/          # 请求DTO
│   └── response/         # 响应DTO
├── security/             # 安全组件
│   ├── JwtTokenProvider  # JWT令牌生成
│   ├── JwtAuthenticationFilter  # JWT认证过滤器
│   ├── UserDetailsImpl   # 用户详情实现
│   └── CustomUserDetailsService # 自定义用户详情服务
├── filter/               # 过滤器
│   └── RequestLoggingFilter # HTTP请求日志过滤器
├── exception/            # 异常处理
└── util/                 # 工具类
    └── RSAUtil            # RSA加密工具类
```

## 核心功能

### 1. 用户认证
- ✅ 用户登录（JWT Token 认证）
- ✅ 用户注册
- ✅ 用户登出
- ✅ Token 刷新
- ✅ 获取当前用户信息

### 2. 用户管理
- ✅ 查询用户信息
- ✅ 用户状态管理
- ✅ 角色权限控制

### 3. 安全特性
- ✅ **RSA密码加密** - 密码传输使用RSA-2048加密
- ✅ BCrypt 密码加密
- ✅ JWT Token 认证
- ✅ 基于角色的访问控制（RBAC）
- ✅ CORS 跨域配置
- ✅ 全局异常处理
- ✅ 请求参数验证
- ✅ HTTP请求日志记录（自动记录所有请求详情）

🔐 **RSA密码加密功能**：系统已实现完整的RSA密码加解密功能，确保密码在传输过程中的安全性。
- **公开公钥端点**：`GET /api/rsa/public-key` 无需登录即可获取公钥，前端可直接调用
- 详细文档：[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#rsa-密码加密)

### 4. 日志监控
- ✅ 请求/响应完整日志
- ✅ 敏感信息自动脱敏
- ✅ 请求耗时统计
- ✅ 错误状态高亮显示
- ✅ 支持路径排除过滤

详细使用指南请查看：[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#请求日志功能)

## 数据库配置

### 1. 使用 Docker Compose（推荐）

项目已配置 Docker Compose，可以一键启动 MySQL、Redis 和 Adminer：

```bash
# 启动数据库服务（MySQL + Redis + Adminer）
docker compose up -d mysql redis adminer
```

启动后可访问：
- **MySQL 管理界面**: http://localhost:8081
- **应用程序**: http://localhost:8080/api

Adminer 登录信息：
- 系统: MySQL
- 服务器: `mysql` (或 `localhost:3306`)
- 用户名: `root` / 密码: `root`

详细使用指南请查看：[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#docker-部署)

### 2. 手动创建数据库

如果选择手动配置数据库：

```sql
CREATE DATABASE financial_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 配置连接

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/financial_app
    username: root
    password: your_password
```

## Redis 配置

确保 Redis 服务已启动，默认配置：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## 快速开始

### 前置要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Gradle 8.x

### 运行项目

1. 克隆项目后，进入项目目录

2. 启动数据库服务（使用 Docker Compose）：
```bash
docker compose up -d mysql redis
```

3. 配置数据库和 Redis 连接信息（如需修改默认配置，编辑 `src/main/resources/application.yml`）

4. 运行项目：
```bash
./gradlew bootRun
```

或在 Windows 中运行：
```bash
gradlew.bat bootRun
```

5. 在 IntelliJ IDEA 中运行 `FinancialAppApplication.kt`

6. 访问 Swagger 文档：
```
http://localhost:8080/api/swagger-ui.html
```

### 快速指南

查看 [开发指南](DEVELOPMENT_GUIDE.md) 了解如何快速使用项目。

## API 接口

完整的 API 接口文档请查看：[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#api-接口文档)

### 快速测试

**用户注册**
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

**用户登录**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**获取当前用户信息**
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

更多接口详情请参考 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#api-接口文档)

## 用户角色

- **ADMIN**: 管理员，拥有所有权限
- **MANAGER**: 经理，拥有部分管理权限
- **USER**: 普通用户，基本访问权限

## 用户状态

- **ACTIVE**: 活跃状态，可以正常使用
- **INACTIVE**: 非活跃状态，无法登录
- **LOCKED**: 锁定状态，无法登录
- **PENDING**: 待审核状态

## 开发指南

### 添加新功能

1. 在 `entity` 包中创建实体类
2. 在 `repository` 包中创建数据访问接口
3. 在 `service` 包中实现业务逻辑
4. 在 `controller` 包中创建控制器
5. 在 `dto` 包中定义请求/响应对象

### 安全最佳实践

- 始终使用 HTTPS 生产环境
- 定期更新 JWT 密钥
- 实现请求限流
- 添加 CSRF 保护
- 定期备份数据库

## 测试

```bash
# 运行所有测试
./gradlew test

# 运行指定测试类
./gradlew test --tests AuthServiceTest
```

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
java -jar build/libs/financial-app-1.0.0.jar
```

## 常见问题

### 1. 数据库连接失败

检查 `application.yml` 中的数据库配置是否正确，确保数据库服务已启动。

### 2. Redis 连接失败

确保 Redis 服务已启动，并检查连接配置。

### 3. Token 验证失败

检查 JWT 密钥配置和 Token 过期时间。

### 4. Gradle 构建错误

如果遇到 Gradle 相关错误，请参考 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#故障排除)。

### 5. 端口占用

如果遇到端口占用问题，请参考 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#故障排除)。

### 更多问题

查看完整的故障排除指南：[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#故障排除)

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 联系方式

如有问题，请提交 Issue 或联系项目维护者。

---

**注意**: 本项目仅用于学习和演示目的，生产环境使用请进行充分的安全加固和性能优化。
