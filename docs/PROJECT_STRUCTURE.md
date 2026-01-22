# 项目结构说明

## 目录结构

```
FinancialApp/
│
├── build.gradle.kts              # Gradle 构建配置文件
├── settings.gradle.kts            # Gradle 设置文件
├── README.md                      # 项目说明文档
├── QUICK_START.md                 # 快速启动指南
├── Dockerfile                     # Docker 镜像构建文件
├── docker-compose.yml             # Docker Compose 配置
├── .env.example                   # 环境变量示例
│
├── database/                      # 数据库脚本目录
│   └── init.sql                  # 数据库初始化脚本
│
├── docs/                         # 文档目录
│   └── api_collection.json       # Postman API 集合
│
├── src/                          # 源代码目录
│   └── main/
│       ├── kotlin/
│       │   └── com/financialapp/
│       │       ├── FinancialAppApplication.kt    # Spring Boot 启动类
│       │       │
│       │       ├── config/       # 配置层
│       │       │   ├── SecurityConfig.kt       # Spring Security 配置
│       │       │   ├── WebConfig.kt            # Web MVC 配置
│       │       │   └── JpaConfig.kt            # JPA 审计配置
│       │       │
│       │       ├── controller/    # 控制层
│       │       │   ├── AuthController.kt       # 认证控制器
│       │       │   └── UserController.kt        # 用户控制器
│       │       │
│       │       ├── service/       # 服务层
│       │       │   ├── AuthService.kt          # 认证服务
│       │       │   └── UserService.kt           # 用户服务
│       │       │
│       │       ├── repository/    # 数据访问层
│       │       │   └── UserRepository.kt         # 用户仓储接口
│       │       │
│       │       ├── entity/        # 实体层
│       │       │   └── User.kt                 # 用户实体
│       │       │
│       │       ├── dto/           # 数据传输对象
│       │       │   ├── request/  # 请求 DTO
│       │       │   │   ├── LoginRequest.kt      # 登录请求
│       │       │   │   └── RegisterRequest.kt   # 注册请求
│       │       │   └── response/ # 响应 DTO
│       │       │       ├── AuthResponse.kt      # 认证响应
│       │       │       └── UserResponse.kt       # 用户响应
│       │       │
│       │       ├── security/      # 安全组件
│       │       │   ├── JwtTokenProvider.kt      # JWT 令牌提供者
│       │       │   ├── JwtAuthenticationFilter.kt # JWT 认证过滤器
│       │       │   ├── UserDetailsImpl.kt       # 用户详情实现
│       │       │   ├── CustomUserDetailsService.kt # 自定义用户详情服务
│       │       │   └── CustomAuthenticationEntryPoint.kt # 自定义认证入口点
│       │       │
│       │       ├── exception/     # 异常处理
│       │       │   ├── BusinessException.kt    # 业务异常
│       │       │   └── GlobalExceptionHandler.kt # 全局异常处理器
│       │       │
│       │       └── util/          # 工具类
│       │           └── ResponseUtil.kt          # 响应工具类
│       │
│       └── resources/             # 资源文件目录
│           ├── application.yml                    # 主配置文件
│           └── application-dev.yml                # 开发环境配置
│
└── out/                          # 输出目录（编译后的文件）
    └── production/               # 生产环境编译输出
        └── FinancialApp/
            ├── MainKt.class     # 旧的测试类（可删除）
            └── META-INF/
                └── FinancialApp.kotlin_module
```

## 各层职责说明

### 1. 配置层 (config)
负责应用程序的各种配置，包括安全配置、Web配置、JPA配置等。

**主要类：**
- `SecurityConfig`: Spring Security 安全配置，定义认证和授权规则
- `WebConfig`: Web MVC 配置，包括 CORS 跨域配置
- `JpaConfig`: JPA 审计配置，自动填充创建时间和更新时间

### 2. 控制层 (controller)
处理 HTTP 请求，接收请求参数，调用服务层处理业务逻辑，返回响应。

**主要类：**
- `AuthController`: 处理登录、注册、登出等认证相关请求
- `UserController`: 处理用户信息查询等请求

**职责：**
- 接收和验证 HTTP 请求
- 参数绑定和验证
- 调用服务层方法
- 构造响应数据
- 处理异常

### 3. 服务层 (service)
实现业务逻辑，协调各个组件完成业务功能。

**主要类：**
- `AuthService`: 认证业务逻辑，包括登录、注册、Token 生成等
- `UserService`: 用户业务逻辑，包括用户创建、查询、更新等

**职责：**
- 实现业务逻辑
- 协调多个 Repository
- 处理事务
- 调用其他服务
- 业务规则验证

### 4. 数据访问层 (repository)
负责与数据库交互，提供数据的增删改查操作。

**主要类：**
- `UserRepository`: 用户数据访问接口

**职责：**
- 定义数据访问方法
- 使用 Spring Data JPA 提供的方法
- 自定义查询方法
- 数据库操作

### 5. 实体层 (entity)
定义数据模型，对应数据库表结构。

**主要类：**
- `User`: 用户实体，包含用户的基本信息和权限信息

**职责：**
- 定义数据结构
- 映射数据库表
- 定义数据关系
- 数据验证

### 6. 数据传输对象 (dto)
定义请求和响应的数据结构，避免直接暴露实体。

**请求 DTO:**
- `LoginRequest`: 登录请求数据
- `RegisterRequest`: 注册请求数据

**响应 DTO:**
- `AuthResponse`: 认证响应数据
- `UserResponse`: 用户响应数据

**职责：**
- 定义 API 接口数据结构
- 数据验证
- 数据转换

### 7. 安全组件 (security)
处理认证和授权相关功能。

**主要类：**
- `JwtTokenProvider`: JWT Token 生成和验证
- `JwtAuthenticationFilter`: JWT 认证过滤器
- `UserDetailsImpl`: Spring Security 用户详情实现
- `CustomUserDetailsService`: 自定义用户详情服务
- `CustomAuthenticationEntryPoint`: 自定义认证失败处理器

**职责：**
- Token 生成和验证
- 用户认证
- 用户授权
- 认证失败处理

### 8. 异常处理 (exception)
统一处理应用程序中的各种异常。

**主要类：**
- `BusinessException`: 业务异常基类
- `GlobalExceptionHandler`: 全局异常处理器

**职责：**
- 定义业务异常
- 统一异常处理
- 错误响应构造
- 日志记录

### 9. 工具类 (util)
提供通用的工具方法。

**主要类：**
- `ResponseUtil`: 响应工具类，统一 API 响应格式

**职责：**
- 提供通用方法
- 辅助功能实现
- 代码复用

## 请求处理流程

### 用户登录流程

```
1. 客户端发送登录请求
   ↓
2. AuthController.login() 接收请求
   ↓
3. AuthService.login() 处理业务逻辑
   ↓
4. AuthenticationManager 执行认证
   ↓
5. CustomUserDetailsService 加载用户信息
   ↓
6. 验证密码
   ↓
7. JwtTokenProvider 生成 JWT Token
   ↓
8. 返回认证响应（包含 Token）
   ↓
9. 客户端收到响应，保存 Token
```

### 受保护接口访问流程

```
1. 客户端发送请求（携带 Token）
   ↓
2. JwtAuthenticationFilter 拦截请求
   ↓
3. JwtTokenProvider 验证 Token
   ↓
4. 加载用户信息到 SecurityContext
   ↓
5. Controller 处理请求
   ↓
6. Service 处理业务逻辑
   ↓
7. Repository 访问数据库
   ↓
8. 返回响应
```

## 数据库设计

### users 表

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT, PRIMARY KEY |
| username | VARCHAR(50) | 用户名 | UNIQUE, NOT NULL |
| password | VARCHAR(255) | 密码（加密） | NOT NULL |
| email | VARCHAR(100) | 邮箱 | UNIQUE, NOT NULL |
| phone | VARCHAR(20) | 手机号 | UNIQUE |
| first_name | VARCHAR(50) | 姓 | NOT NULL |
| last_name | VARCHAR(50) | 名 | NOT NULL |
| role | VARCHAR(20) | 角色 | NOT NULL |
| enabled | BOOLEAN | 是否启用 | NOT NULL, DEFAULT TRUE |
| status | VARCHAR(20) | 状态 | NOT NULL |
| avatar | VARCHAR(500) | 头像 | DEFAULT 'default-avatar.png' |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |
| last_login_at | TIMESTAMP | 最后登录时间 | |
| last_login_ip | VARCHAR(100) | 最后登录IP | |

### 索引

- idx_username: username 字段唯一索引
- idx_email: email 字段唯一索引
- idx_phone: phone 字段唯一索引

## 扩展建议

### 添加新功能的步骤

1. **创建实体类**（如果需要新的数据表）
   - 在 `entity` 包中创建实体类
   - 使用 JPA 注解定义表结构

2. **创建 Repository**
   - 在 `repository` 包中创建接口
   - 继承 `JpaRepository`

3. **创建 DTO**
   - 在 `dto/request` 中创建请求 DTO
   - 在 `dto/response` 中创建响应 DTO

4. **创建 Service**
   - 在 `service` 包中创建服务类
   - 实现业务逻辑
   - 使用 `@Service` 注解

5. **创建 Controller**
   - 在 `controller` 包中创建控制器
   - 使用 `@RestController` 注解
   - 定义接口路径和方法

6. **添加异常处理**
   - 如需要，在 `exception` 包中添加自定义异常
   - 在 `GlobalExceptionHandler` 中添加处理逻辑

7. **配置权限**
   - 在 `SecurityConfig` 中配置接口权限
   - 或使用 `@PreAuthorize` 注解

### 性能优化建议

1. **数据库优化**
   - 添加适当的索引
   - 优化查询语句
   - 使用连接池（HikariCP）

2. **缓存优化**
   - 使用 Redis 缓存热点数据
   - 实现 Spring Cache 注解

3. **异步处理**
   - 使用 Kotlin Coroutines
   - 或使用 Spring `@Async`

4. **分页查询**
   - 使用 Spring Data JPA 的 `Pageable`
   - 避免大量数据一次性加载

## 安全建议

1. **密码安全**
   - 使用 BCrypt 加密
   - 要求复杂密码
   - 定期更换密码

2. **Token 安全**
   - 使用 HTTPS
   - 设置合理的过期时间
   - 实现 Token 刷新机制

3. **输入验证**
   - 严格验证所有输入
   - 防止 SQL 注入
   - 防止 XSS 攻击

4. **访问控制**
   - 最小权限原则
   - 定期审计权限
   - 敏感操作需要二次验证
