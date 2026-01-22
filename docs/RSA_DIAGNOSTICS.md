# RSA 加密解密诊断指南

## 问题：`javax.crypto.BadPaddingException: Decryption error`

### 🔍 常见原因

#### 1. **密钥不匹配（最常见）**
```
场景：前端用旧的公钥加密，后端用新的私钥解密

原因：
- 应用重启后生成了新的临时密钥对
- 前端缓存了旧的公钥
- 前端没有重新获取公钥

解决：
1. 每次应用重启后，前端必须重新获取公钥
2. 或者将密钥配置到 application.yml 中，避免每次重启生成新密钥
```

#### 2. **JSEncrypt 与 Java RSA 格式不兼容**
```
场景：前端 JSEncrypt 使用某种格式，后端 Java RSA 无法识别

原因：
- JSEncrypt 可能对 PEM 格式有特殊要求
- 公钥传输过程中被截断或损坏

解决：
1. 检查前端获取的公钥格式是否完整
2. 确保公钥包含完整的 BEGIN/END 标记
```

#### 3. **密文传输过程中被篡改**
```
场景：密文在 HTTP 传输过程中被修改

原因：
- URL 编码问题
- Base64 解码问题
- 字符编码问题

解决：
1. 确保 HTTP Content-Type 正确（application/json）
2. 确保 Base64 编解码正确
```

---

## ✅ 诊断步骤

### 步骤 1：检查应用启动日志

查看应用启动时的日志，确认 RSA 密钥是否生成：

```
临时RSA密钥对已生成
公钥: MIIBIjANBgkqhkiG9w0...
私钥: MIIEvQIBADANBgkqhki...
```

**如果是临时密钥**：
- 每次应用重启都会生成新密钥
- **前端必须重新获取公钥**

---

### 步骤 2：前端诊断

打开浏览器开发者工具（F12），查看 Network 标签：

#### 2.1 检查获取公钥请求

请求：`GET /api/rsa/public-key`

检查响应：
```json
{
  "success": true,
  "data": {
    "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\n-----END PUBLIC KEY-----",
    "algorithm": "RSA",
    "keySize": "2048"
  }
}
```

**验证要点**：
- ✅ 公钥包含完整的 `-----BEGIN PUBLIC KEY-----` 和 `-----END PUBLIC KEY-----`
- ✅ 公钥不是空的或过短的

#### 2.2 检查登录请求

请求：`POST /api/auth/login`

检查请求体：
```json
{
  "username": "admin",
  "password": "Y3B6bGh5YW...（加密后的密文）",
  "encrypted": true
}
```

**验证要点**：
- ✅ `encrypted` 字段为 `true`
- ✅ `password` 字段是 Base64 编码的密文（不是明文）

---

### 步骤 3：后端诊断

查看后端日志（启用 DEBUG 级别）：

```yaml
logging:
  level:
    com.financialapp.service.RSAService: DEBUG
    com.financialapp.service.AuthService: DEBUG
```

#### 3.1 查看解密日志

```
开始解密密码，密文长度: 344
密文前50个字符: Y3B6bGh5YW53cmVtYWlsQGV4YW1wbGUuY29t...
```

#### 3.2 查看错误日志

**如果出现 `BadPaddingException`**：
```
BadPaddingException: 密钥不匹配或密文被篡改
```

**这意味着**：前端使用的公钥与后端使用的私钥不匹配！

---

## 🔧 解决方案

### 方案 1：每次启动后重新获取公钥（推荐用于开发）

1. 停止应用
2. 启动应用
3. 在前端页面点击"获取公钥"按钮
4. 输入密码（会自动加密）
5. 点击登录

---

### 方案 2：配置持久化密钥（推荐用于生产）

#### 2.1 生成密钥对

启动应用，从日志中复制生成的临时密钥：

```
临时RSA密钥对已生成
公钥: -----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
-----END PUBLIC KEY-----

私钥: -----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
-----END PRIVATE KEY-----
```

#### 2.2 添加到 application.yml

```yaml
rsa:
  public-key: |
    -----BEGIN PUBLIC KEY-----
    MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
    (完整公钥内容)
    -----END PUBLIC KEY-----

  private-key: |
    -----BEGIN PRIVATE KEY-----
    MIIEvQIBADANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
    (完整私钥内容)
    -----END PRIVATE KEY-----
```

**注意**：
- 使用 `|` 符号支持多行字符串
- 保持公钥和私钥的完整格式
- 包含 BEGIN/END 标记

#### 2.3 重新启动应用

配置密钥后，应用将使用固定密钥对，不再每次启动都生成新密钥。

---

### 方案 3：使用明文密码测试（仅用于调试）

在前端页面取消勾选"加密密码"选项：

1. 取消勾选"加密密码"复选框
2. 输入明文密码
3. 点击登录

**注意**：此方法仅用于调试，生产环境必须使用加密密码！

---

## 🧪 测试验证

### 测试 1：验证密钥一致性

```bash
# 1. 启动应用
# 2. 在浏览器中访问登录测试页面
# 3. 点击"获取公钥"
# 4. 复制公钥

# 5. 在应用日志中查找公钥（与前端获取的应该一致）
# 6. 使用相同的公钥加密密码
# 7. 发送登录请求
```

### 测试 2：验证加密解密流程

```bash
# 使用 curl 测试

# 1. 获取公钥
curl http://localhost:8080/api/rsa/public-key

# 2. 使用公钥加密密码（需要使用加密工具或前端页面）
# 3. 发送登录请求
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "加密后的密码",
    "encrypted": true
  }'
```

---

## 📋 检查清单

在报告问题时，请确认：

- [ ] 应用是否刚启动过？
- [ ] 前端是否重新获取了公钥？
- [ ] 前端是否勾选了"加密密码"？
- [ ] 公钥格式是否完整（包含 BEGIN/END 标记）？
- [ ] 密文长度是否合理（通常 > 100 字符）？
- [ ] 后端日志中是否有详细的错误信息？

---

## 🆘 常见错误

### 错误 1：`javax.crypto.BadPaddingException: Decryption error`

**原因**：密钥不匹配

**解决**：重新获取公钥，重新加密密码

---

### 错误 2：`javax.crypto.IllegalBlockSizeException: Input length must be multiple of 8`

**原因**：密文不是有效的 Base64 编码

**解决**：检查密文格式，确保是完整的 Base64 字符串

---

### 错误 3：`IllegalArgumentException: 加密密码不能为空`

**原因**：前端没有正确发送加密密码

**解决**：检查前端是否正确加密了密码

---

## 💡 最佳实践

1. **生产环境必须使用固定密钥**：将密钥配置到 application.yml 或通过环境变量注入
2. **密钥不要提交到版本控制**：使用环境变量或密钥管理系统
3. **定期轮换密钥**：增强安全性
4. **前端每次打开页面都重新获取公钥**：避免使用缓存的旧公钥
5. **使用 HTTPS**：防止中间人攻击

---

## 📞 获取帮助

如果以上方案都无法解决问题，请提供：

1. 应用启动日志（包含 RSA 密钥生成部分）
2. 浏览器 Network 标签中的：
   - 获取公钥的请求和响应
   - 登录请求的请求体
3. 后端完整错误日志（包括堆栈跟踪）

---

**最后更新**：2024-01-16
**文档版本**：1.0
