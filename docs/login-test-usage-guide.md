# Login-Test.html 使用指南

## 📋 概述

`login-test.html` 是 FinancialApp 项目的认证接口测试工具，提供完整的登录、注册、用户中心功能，支持 RSA 加密和明文密码两种模式。

## 🚀 访问地址

- **本地开发环境**: http://localhost:8080/login-test.html
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html

## ✨ 功能特性

### 1. 登录功能

#### 基本信息
- **接口**: `POST /api/auth/login`
- **支持模式**: RSA 加密 / 明文密码
- **Token 存储**: 自动保存到 localStorage

#### 使用步骤

1. **填写登录信息**
   ```
   API 基础地址: http://localhost:8080/api
   用户名: admin（或其他已注册用户）
   密码: admin123（或其他密码）
   ```

2. **选择加密模式**
   - ✅ **加密密码**（推荐）:
     - 点击"获取公钥"按钮
     - 系统自动获取服务器公钥并加密密码
     - 加密后的密码会显示在"加密后的密码"字段
   - ⚪ **明文密码**（仅测试）:
     - 直接发送原始密码（不推荐生产环境使用）

3. **点击登录**
   - 发送请求到 `/api/auth/login`
   - 成功后会显示响应信息
   - Token 自动保存到 localStorage

4. **登录成功后**
   - "登出"按钮会显示
   - 可以切换到"用户中心"查看信息

#### 请求体示例

**RSA 加密模式**:
```json
{
  "username": "admin",
  "password": "加密后的Base64字符串",
  "encrypted": true
}
```

**明文密码模式**:
```json
{
  "username": "admin",
  "password": "admin123",
  "encrypted": false
}
```

#### 响应示例

```json
{
  "timestamp": "2026-01-16T15:48:52.1026535",
  "status": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com"
    }
  }
}
```

---

### 2. 注册功能

#### 基本信息
- **接口**: `POST /api/auth/register`
- **必填字段**: 用户名、邮箱、密码、确认密码、姓、名
- **可选字段**: 手机号
- **支持模式**: RSA 加密 / 明文密码

#### 字段验证规则

| 字段 | 验证规则 | 错误提示 |
|------|----------|----------|
| 用户名 | 3-50字符，只能包含字母、数字、下划线 | "用户名只能包含字母、数字和下划线" |
| 邮箱 | 标准邮箱格式 | "邮箱格式不正确" |
| 密码 | 至少6位 | "密码至少6位" |
| 确认密码 | 必须与密码一致 | "两次输入的密码不一致" |
| 姓 | 不能为空，最多50字符 | "请输入姓" |
| 名 | 不能为空，最多50字符 | "请输入名" |
| 手机号 | 可选，中国大陆11位数字 | "手机号格式不正确" |

#### 使用步骤

1. **切换到注册页面**
   - 点击"注册"按钮

2. **填写注册信息**
   ```
   API 基础地址: http://localhost:8080/api
   用户名: newuser
   邮箱: newuser@example.com
   密码: password123
   确认密码: password123
   手机号: 13800138000（可选）
   姓: 张
   名: 三
   ```

3. **选择加密模式**
   - ✅ **加密密码**（推荐）:
     - 点击"获取公钥"按钮
     - 系统自动加密密码
   - ⚪ **明文密码**（仅测试）

4. **点击注册**
   - 发送请求到 `/api/auth/register`
   - 成功后自动切换到登录页面并填充用户名

#### 请求体示例

**RSA 加密模式**:
```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "加密后的Base64字符串",
  "confirmPassword": "加密后的Base64字符串",
  "firstName": "张",
  "lastName": "三",
  "phone": "13800138000",
  "encrypted": true
}
```

**明文密码模式**:
```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "firstName": "张",
  "lastName": "三",
  "phone": "13800138000",
  "encrypted": false
}
```

#### 响应示例

```json
{
  "timestamp": "2026-01-16T15:48:52.1026535",
  "status": 201,
  "message": "注册成功",
  "data": {
    "id": 2,
    "username": "newuser",
    "email": "newuser@example.com",
    "firstName": "张",
    "lastName": "三",
    "phone": "13800138000",
    "status": "ACTIVE",
    "enabled": true
  }
}
```

---

### 3. 用户中心功能

#### 基本功能

用户中心提供以下功能：

1. **查看 Token 信息**
   - JWT Access Token
   - Refresh Token

2. **获取用户信息**
   - 接口: `GET /api/auth/me`
   - 需要认证

3. **刷新 Token**
   - 接口: `POST /api/auth/refresh`
   - 需要认证
   - 使用当前 Token 获取新的 Access Token

4. **复制 Token**
   - 一键复制 Access Token 到剪贴板

5. **清除 Token**
   - 清除本地存储的 Token
   - 需要重新登录

#### 使用步骤

1. **切换到用户中心**
   - 点击"用户中心"按钮

2. **查看 Token 信息**
   - Access Token 和 Refresh Token 会自动显示

3. **获取用户信息**
   - 点击"获取用户信息"按钮
   - 显示当前登录用户的详细信息

4. **刷新 Token**
   - 点击"刷新Token"按钮
   - 系统自动获取新的 Access Token
   - 自动更新本地存储

5. **复制 Token**
   - 点击"📋 复制Token"按钮
   - Token 复制到剪贴板，方便在其他地方使用

6. **清除 Token**
   - 点击"🗑️ 清除Token"按钮
   - 确认后清除本地存储的 Token

---

## 🔧 常见问题

### 1. 登录失败

**错误信息**: "用户名或密码错误"

**可能原因**:
- 用户名或密码错误
- 密码在数据库中不是 BCrypt 格式
- 用户被禁用

**解决方案**:
1. 检查用户名和密码是否正确
2. 检查用户状态是否为 ACTIVE
3. 如果密码不是 BCrypt 格式，使用 `generateBcryptPassword` 任务生成新密码

**诊断步骤**:
```sql
-- 检查用户状态
SELECT username, enabled, status,
       CASE
           WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt格式 ✅'
           ELSE '非BCrypt格式 ❌'
       END AS password_format
FROM users
WHERE username = 'your_username';
```

---

### 2. 获取公钥失败

**错误信息**: "获取公钥失败: 404"

**可能原因**:
- RSA 服务未启动
- 路径配置错误

**解决方案**:
1. 检查 RSAController 是否正确配置
2. 检查 `/api/rsa/public-key` 接口是否可访问

---

### 3. 注册失败

**常见错误**:

1. **用户名已存在**
   ```
   "用户名已被注册"
   ```

2. **邮箱已存在**
   ```
   "邮箱已被注册"
   ```

3. **验证失败**
   ```
   "请求参数验证失败"
   ```

**解决方案**:
1. 使用不同的用户名和邮箱
2. 检查所有必填字段是否正确填写
3. 确保密码和确认密码一致

---

### 4. Token 过期

**错误信息**: "Token已过期" / "Invalid token"

**解决方案**:
1. 在用户中心点击"刷新Token"
2. 如果刷新失败，需要重新登录
3. 检查 Token 过期时间配置

---

### 5. 跨域问题 (CORS)

**错误信息**: "Access to fetch at '...' has been blocked by CORS policy"

**解决方案**:
1. 确保后端 CORS 配置正确
2. 检查 `SecurityConfig.kt` 中的 CORS 配置
3. 使用浏览器插件临时解决（仅开发环境）

---

## 📝 最佳实践

### 1. 密码加密

**生产环境必须使用 RSA 加密**:
```javascript
// ✅ 推荐
encryptPassword = true

// ❌ 不推荐（仅开发测试）
encryptPassword = false
```

### 2. Token 管理

- Token 自动保存到 localStorage
- 定期刷新 Token（推荐在过期前刷新）
- 不要在 URL 中传递 Token
- 使用 HTTPS 保护传输安全

### 3. 用户注册

- 使用强密码（至少8位，包含大小写字母、数字、特殊字符）
- 填写正确的邮箱用于找回密码
- 手机号可选，但建议填写用于安全验证

---

## 🧪 测试场景

### 场景 1: 正常登录流程

1. 打开 login-test.html
2. 输入用户名 `admin` 和密码 `admin123`
3. 勾选"加密密码"
4. 点击"登录"
5. 验证登录成功并获取 Token

### 场景 2: 注册新用户

1. 切换到"注册"页面
2. 填写完整的注册信息
3. 点击"获取公钥"
4. 点击"注册"
5. 验证注册成功
6. 自动切换到登录页面，使用新用户登录

### 场景 3: Token 刷新

1. 登录成功
2. 切换到"用户中心"
3. 点击"获取用户信息"
4. 点击"刷新Token"
5. 验证 Token 已更新

### 场景 4: 明文密码登录（测试用）

1. 输入用户名和密码
2. 取消勾选"加密密码"
3. 点击"登录"
4. 验证登录成功

---

## 🛡️ 安全注意事项

1. **不要在生产环境使用明文密码**
2. **不要将 login-test.html 部署到生产环境**
3. **定期更改管理员密码**
4. **使用 HTTPS 保护 Token 传输**
5. **不要在浏览器控制台暴露敏感信息**

---

## 📚 相关文档

- [API 文档 (Swagger)](http://localhost:8080/api/swagger-ui.html)
- [开发指南](DEVELOPMENT_GUIDE.md)
- [登录故障排查](login-failure-troubleshooting.md)
- [快速修复用户 abc](quick-fix-abc-user.md)

---

## 📞 联系支持

如有问题，请联系开发团队或查看项目文档。

**最后更新**: 2026-01-16
