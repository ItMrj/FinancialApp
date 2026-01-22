# 用户 abc 登录问题 - 快速修复指南

## 问题总结
用户 `abc` 在数据库中存在，但登录时返回 `"用户名或密码错误"`。

## 问题诊断结果

经过代码审查，我发现以下可能的问题：

### 1. 密码格式问题（最可能）
用户 `abc` 的密码可能不是 **BCrypt 加密格式**。

**BCrypt 密码特征**：
- 必须以 `$2a$` 或 `$2b$` 开头
- 示例：`$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`

### 2. 登录请求 encrypted 标志不正确
- `encrypted=true`（默认）：密码应该用 RSA 加密
- `encrypted=false`：密码是明文

如果数据库中的密码是明文，但登录时设置了 `encrypted=true`，会导致：
1. 后端尝试解密明文 → 失败
2. 或者解密后的密码与数据库中的明文不匹配

## 快速修复步骤

### 步骤 1：检查用户 abc 的密码格式

运行 SQL 查询：
```sql
SELECT
    username,
    password,
    enabled,
    status,
    CASE
        WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt格式 ✅'
        ELSE '非BCrypt格式（可能是明文）❌'
    END AS password_format
FROM users
WHERE username = 'abc';
```

**结果判断**：
- ✅ 如果显示"BCrypt格式" → 进入步骤 3
- ❌ 如果显示"非BCrypt格式" → 进入步骤 2

### 步骤 2：修复密码格式

#### 方法 A：使用 BCryptPasswordGenerator 工具（推荐）

1. 运行 `BCryptPasswordGenerator.kt` 的 main 方法：
```bash
cd "g:/My Service/FinancialApp"
./gradlew test --tests "com.financialapp.util.BCryptPasswordGenerator"
```

2. 或者直接在 IDE 中运行 `BCryptPasswordGenerator.main()`

3. 输入明文密码（例如 `abc123`），获取 BCrypt 哈希

4. 更新数据库：
```sql
UPDATE users
SET password = '生成的BCrypt哈希值',
    enabled = TRUE,
    status = 'ACTIVE',
    updated_at = NOW()
WHERE username = 'abc';
```

#### 方法 B：使用在线工具

访问 https://bcrypt-generator.com/ 输入明文密码，获取 BCrypt 哈希。

#### 方法 C：使用明文密码（向后兼容）

如果数据库中的密码是明文（例如 `abc123`），可以在登录时设置 `encrypted: false`：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "abc",
    "password": "abc123",
    "encrypted": false
  }'
```

或者使用 login-test.html：
1. 打开 http://localhost:8080/login-test.html
2. 取消勾选"加密密码"复选框
3. 输入用户名 `abc` 和明文密码
4. 点击"登录"

### 步骤 3：验证修复

#### 方式 A：使用 login-test.html

1. 打开 http://localhost:8080/login-test.html
2. 输入用户名 `abc` 和密码
3. 如果密码是 BCrypt 格式：
   - 勾选"加密密码"复选框
   - 点击"获取公钥"
   - 点击"登录"
4. 如果密码是明文：
   - 取消勾选"加密密码"复选框
   - 直接点击"登录"

#### 方式 B：使用 curl 测试

```bash
# 测试明文密码模式
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "abc",
    "password": "abc123",
    "encrypted": false
  }'

# 成功响应示例：
# {
#   "success": true,
#   "message": "登录成功",
#   "data": {
#     "accessToken": "...",
#     "refreshToken": "...",
#     "tokenType": "Bearer",
#     "expiresIn": 86400000,
#     "user": {...}
#   }
# }
```

### 步骤 4：检查用户状态

确保用户的 enabled 和 status 字段正确：

```sql
SELECT username, enabled, status
FROM users
WHERE username = 'abc';

-- 结果应该是：
-- username = 'abc'
-- enabled = TRUE
-- status = 'ACTIVE'
```

如果不是，更新状态：
```sql
UPDATE users
SET enabled = TRUE,
    status = 'ACTIVE',
    updated_at = NOW()
WHERE username = 'abc';
```

## 代码改进

我已经添加了详细的调试日志到 `AuthService.kt`：

```kotlin
logger.debug("登录请求 - 用户名: {}, 密码是否加密: {}", request.username, request.encrypted)
logger.debug("密码解密成功，长度: {}", decrypted.length)
logger.debug("认证成功: {}", request.username)
logger.debug("用户状态检查 - enabled: {}, status: {}", user.enabled, user.status)
logger.info("用户 {} 登录成功", request.username)
```

重启应用程序后，登录时会输出详细的日志，帮助定位问题。

## 预防措施

1. **使用注册接口创建用户**：不要直接在数据库中插入用户
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "username": "newuser",
       "email": "newuser@example.com",
       "password": "password123",
       "confirmPassword": "password123",
       "firstName": "New",
       "lastName": "User",
       "encrypted": false
     }'
   ```

2. **使用 BCrypt 加密所有密码**：确保密码通过 `PasswordEncoder.encode()` 加密

3. **使用 Flyway 管理数据库**：避免手动修改数据库

4. **检查脚本**：已创建 `database/check-and-fix-user-abc.sql` 用于检查所有用户的状态

## 相关文件

- `src/main/kotlin/com/financialapp/service/AuthService.kt` - 已添加详细日志
- `database/check-and-fix-user-abc.sql` - 数据库检查脚本
- `docs/login-failure-troubleshooting.md` - 完整诊断指南
- `src/test/kotlin/com/financialapp/util/BCryptPasswordGenerator.kt` - BCrypt 密码生成工具

## 联系支持

如果以上步骤无法解决问题，请提供以下信息：
1. 用户 abc 的密码格式（从数据库查询）
2. 登录时发送的请求体（从 login-test.html 查看）
3. 应用程序日志（特别是登录时的调试日志）

## 总结

**最可能的原因**：用户 abc 的密码不是 BCrypt 加密格式。

**快速解决方案**：
1. 查询用户 abc 的密码，检查是否以 `$2a$` 或 `$2b$` 开头
2. 如果不是，使用 BCrypt 工具重新生成密码哈希
3. 更新数据库中的密码字段
4. 使用 login-test.html 的明文模式测试（encrypted: false）
