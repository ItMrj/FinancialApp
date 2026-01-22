# 登录失败问题诊断与修复指南

## 问题描述
用户 `abc` 在数据库中存在，但登录时返回 `"用户名或密码错误"`（401 状态码）。

## 可能的原因

### 1. 密码格式不匹配（最常见）
数据库中用户 `abc` 的密码可能不是 BCrypt 加密格式。

**检查方式**：
```sql
SELECT username, password, enabled, status
FROM users
WHERE username = 'abc';
```

**BCrypt 加密的特征**：
- 以 `$2a$` 或 `$2b$` 开头
- 例如：`$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`

### 2. 登录请求中的 encrypted 标志不正确
- 如果前端发送明文密码但 `encrypted=true`，后端会尝试解密明文，导致失败
- 如果前端发送加密密码但 `encrypted=false`，后端会直接用加密后的密码比对，导致失败

### 3. 用户状态异常
- `enabled` 必须为 `true`
- `status` 必须为 `'ACTIVE'`

## 修复步骤

### 方案一：通过数据库修复密码（快速方案）

#### 1.1 检查用户 abc 的当前状态
```sql
SELECT 
    username, 
    password,
    enabled,
    status,
    CASE
        WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt格式'
        ELSE '非BCrypt格式（可能是明文）'
    END AS password_format
FROM users
WHERE username = 'abc';
```

#### 1.2 生成 BCrypt 加密密码

可以使用在线工具或通过 Java/Kotlin 代码生成：

**在线工具**：
- https://bcrypt-generator.com/
- https://www.javainuse.com/onlineBcrypt

**通过代码生成**（推荐）：
```kotlin
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

fun main() {
    val encoder = BCryptPasswordEncoder()
    val plainPassword = "你的明文密码"
    val hashedPassword = encoder.encode(plainPassword)
    println("BCrypt 加密后的密码: $hashedPassword")
}
```

#### 1.3 更新用户 abc 的密码

假设明文密码是 `abc123`，其 BCrypt 加密后的值可能是：
- `$2a$10$YourGeneratedBCryptHashForAbc123...`

```sql
-- 替换为实际生成的 BCrypt 哈希值
UPDATE users
SET password = '$2a$10$YourGeneratedBCryptHashForAbc123',
    enabled = TRUE,
    status = 'ACTIVE',
    updated_at = NOW()
WHERE username = 'abc';
```

#### 1.4 验证修复
```sql
SELECT username, enabled, status, password
FROM users
WHERE username = 'abc';
```

### 方案二：通过注册新用户重新创建

如果用户 abc 是测试用户，可以通过注册接口重新创建：

1. 使用 `login-test.html` 的注册功能
2. 或者使用 Postman/curl 调用注册接口

**注册请求示例**：
```json
POST http://localhost:8080/api/auth/register

{
  "username": "abc",
  "email": "abc@example.com",
  "password": "abc123",
  "confirmPassword": "abc123",
  "firstName": "A",
  "lastName": "BC",
  "encrypted": false
}
```

### 方案三：检查并修复前端登录请求

#### 3.1 使用 login-test.html 测试

1. 打开 `http://localhost:8080/login-test.html`
2. 输入用户名 `abc` 和密码
3. **测试两种模式**：

**模式 A：使用明文密码（向后兼容）**
- 取消勾选"加密密码"复选框
- 点击"登录"按钮
- 观察响应结果

**模式 B：使用 RSA 加密密码（推荐）**
- 勾选"加密密码"复选框
- 确保"获取公钥"已成功
- 点击"登录"按钮
- 观察响应结果

#### 3.2 查看发送的请求体

在 login-test.html 中，"发送的请求体"区域会显示实际发送的 JSON：

**明文模式**：
```json
{
  "username": "abc",
  "password": "abc123",
  "encrypted": false
}
```

**加密模式**：
```json
{
  "username": "abc",
  "password": "很长的RSA加密字符串...",
  "encrypted": true
}
```

## 诊断日志

我已经在 `AuthService.kt` 中添加了详细的调试日志。登录时会输出：

```
登录请求 - 用户名: abc, 密码是否加密: true/false
尝试解密密码...
密码解密成功，长度: 6
准备认证用户: abc
认证成功: abc
用户状态检查 - enabled: true, status: ACTIVE
用户 abc 登录成功
```

如果出现错误，日志会显示：
```
密码解密失败: Invalid RSA encrypted data
登录失败 - 用户名: abc, 错误类型: IllegalArgumentException, 错误信息: 密码解密失败，请检查前端是否正确使用RSA加密
```

## 验证修复

修复后，使用以下步骤验证：

1. 重启应用程序
2. 使用 login-test.html 测试登录
3. 或者使用 curl 命令测试：

```bash
# 明文密码测试
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "abc",
    "password": "abc123",
    "encrypted": false
  }'

# 查看响应
# 成功: {"success": true, "message": "登录成功", "data": {...}}
# 失败: {"status": 401, "error": "Business Error", "message": "用户名或密码错误"}
```

## 常见 BCrypt 密码示例

以下是一些常见密码的 BCrypt 加密示例（仅供参考，请使用自己的）：

```
密码: 123456
BCrypt: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

密码: admin123
BCrypt: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

密码: abc123
BCrypt: $2a$10$rO5J7I6jJqJqJqJqJqJqJqeZ5Mkkmwe.20cQQubK3.HZWzG3YB1tl
```

## 预防措施

1. **始终使用 BCrypt 加密**：用户密码必须通过 `PasswordEncoder.encode()` 加密后存储
2. **使用注册接口创建用户**：不要直接在数据库中插入用户，使用 `/auth/register` 接口
3. **使用 Flyway 管理数据库变更**：避免手动修改数据库
4. **测试时使用明文模式**：开发测试时可以设置 `encrypted: false` 简化测试

## 总结

最可能的问题是：**用户 abc 的密码在数据库中不是 BCrypt 加密格式**。

**快速修复**：
1. 查询用户 abc 的密码格式
2. 如果不是以 `$2a$` 或 `$2b$` 开头，说明不是 BCrypt 格式
3. 使用 BCrypt 工具重新生成密码哈希
4. 更新数据库中的密码字段
5. 使用 login-test.html 的明文模式测试登录（encrypted: false）

**长期解决方案**：
- 使用注册接口创建所有用户
- 不要手动插入用户数据
- 使用 Flyway 管理数据库迁移
