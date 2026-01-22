# RSA密码加密实现指南

## 概述

本系统使用RSA非对称加密技术对用户密码进行加密传输，确保密码在传输过程中的安全性。

## 安全架构

```
前端（客户端）                    后端（服务端）
    |                                |
    |-- 获取RSA公钥 ---------------->|
    |<--- 返回公钥 --------------------|
    |                                |
    |-- 使用公钥加密密码 ------------->|
    |                                |-- 使用私钥解密密码
    |                                |-- 使用BCrypt哈希存储
    |                                |-- 验证密码
    |                                |
```

## 密码处理流程

### 1. 注册流程
1. 前端：获取RSA公钥 → 使用公钥加密密码 → 发送注册请求
2. 后端：使用私钥解密密码 → BCrypt哈希加密 → 存储到数据库

### 2. 登录流程
1. 前端：获取RSA公钥 → 使用公钥加密密码 → 发送登录请求
2. 后端：使用私钥解密密码 → 与数据库中的BCrypt哈希比对 → 返回认证结果

## API接口

### 获取RSA公钥

**请求**
```
GET /api/rsa/public-key
```

**响应**
```json
{
  "success": true,
  "data": {
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
    "algorithm": "RSA",
    "keySize": "2048"
  }
}
```

### 登录接口（加密密码）

**请求**
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "加密后的密码字符串",
  "encrypted": true
}
```

**响应**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      ...
    }
  }
}
```

### 注册接口（加密密码）

**请求**
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "加密后的密码字符串",
  "confirmPassword": "加密后的确认密码字符串",
  "email": "user@example.com",
  "firstName": "张",
  "lastName": "三",
  "encrypted": true
}
```

## 前端实现示例

### JavaScript (使用 JSEncrypt)

```html
<!-- 引入 JSEncrypt 库 -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/jsencrypt/3.3.2/jsencrypt.min.js"></script>

<script>
const API_BASE_URL = 'http://localhost:8080/api';
let publicKey = '';

// 1. 获取公钥
async function getPublicKey() {
  const response = await fetch(`${API_BASE_URL}/rsa/public-key`);
  const data = await response.json();
  publicKey = data.data.publicKey;
  return publicKey;
}

// 2. 加密密码
function encryptPassword(plainPassword, publicKey) {
  const encrypt = new JSEncrypt();
  encrypt.setPublicKey(publicKey);
  return encrypt.encrypt(plainPassword);
}

// 3. 登录
async function login(username, plainPassword) {
  // 获取公钥
  const publicKey = await getPublicKey();

  // 加密密码
  const encryptedPassword = encryptPassword(plainPassword, publicKey);

  // 发送登录请求
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      username: username,
      password: encryptedPassword,
      encrypted: true  // 标识密码已加密
    })
  });

  return await response.json();
}

// 使用示例
login('admin', 'Test@123456')
  .then(data => console.log(data))
  .catch(error => console.error(error));
</script>
```

### Vue.js 示例

```javascript
// 安装依赖
// npm install jsencrypt

import JSEncrypt from 'jsencrypt'

export default {
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      publicKey: ''
    }
  },
  async created() {
    await this.getPublicKey()
  },
  methods: {
    async getPublicKey() {
      const response = await this.$http.get('/rsa/public-key')
      this.publicKey = response.data.data.publicKey
    },

    encryptPassword(plainPassword) {
      const encrypt = new JSEncrypt()
      encrypt.setPublicKey(this.publicKey)
      return encrypt.encrypt(plainPassword)
    },

    async handleLogin() {
      const encryptedPassword = this.encryptPassword(this.loginForm.password)

      await this.$http.post('/auth/login', {
        username: this.loginForm.username,
        password: encryptedPassword,
        encrypted: true
      })
    }
  }
}
```

### React 示例

```javascript
// 安装依赖
// npm install jsencrypt

import JSEncrypt from 'jsencrypt';
import { useEffect, useState } from 'react';

function LoginForm() {
  const [publicKey, setPublicKey] = useState('');
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });

  useEffect(() => {
    fetchPublicKey();
  }, []);

  const fetchPublicKey = async () => {
    const response = await fetch('/api/rsa/public-key');
    const data = await response.json();
    setPublicKey(data.data.publicKey);
  };

  const encryptPassword = (plainPassword) => {
    const encrypt = new JSEncrypt();
    encrypt.setPublicKey(publicKey);
    return encrypt.encrypt(plainPassword);
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    const encryptedPassword = encryptPassword(formData.password);

    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: formData.username,
        password: encryptedPassword,
        encrypted: true
      })
    });
  };

  return (
    <form onSubmit={handleLogin}>
      <input
        type="text"
        value={formData.username}
        onChange={(e) => setFormData({...formData, username: e.target.value})}
      />
      <input
        type="password"
        value={formData.password}
        onChange={(e) => setFormData({...formData, password: e.target.value})}
      />
      <button type="submit">登录</button>
    </form>
  );
}
```

### Axios 封装示例

```javascript
// src/utils/http.js
import axios from 'axios'
import JSEncrypt from 'jsencrypt'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

let publicKey = ''

// 获取公钥
export async function getPublicKey() {
  if (!publicKey) {
    const response = await http.get('/rsa/public-key')
    publicKey = response.data.data.publicKey
  }
  return publicKey
}

// 加密密码
export function encryptPassword(plainPassword) {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKey)
  return encrypt.encrypt(plainPassword)
}

// 登录
export async function login(username, password) {
  await getPublicKey()
  const encryptedPassword = encryptPassword(password)
  return http.post('/auth/login', {
    username,
    password: encryptedPassword,
    encrypted: true
  })
}

export default http
```

## 后端配置

### 生成RSA密钥对

运行提供的批处理脚本生成密钥对：

```bash
generate-rsa-keys.bat
```

生成的密钥对会显示在控制台，将其复制到 `application.yml`：

```yaml
# RSA加密配置
rsa:
  public-key: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
  private-key: MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...
```

### 环境变量配置

为了安全性，推荐使用环境变量配置RSA密钥：

```yaml
rsa:
  public-key: ${RSA_PUBLIC_KEY}
  private-key: ${RSA_PRIVATE_KEY}
```

## 测试工具

项目提供了完整的测试页面：

1. **test-rsa-encryption.html**：完整的RSA加密测试页面
2. 在浏览器中打开该页面，即可测试：
   - 获取公钥
   - 加密密码
   - 测试登录

## 安全注意事项

### 1. 密钥管理
- ✅ 将RSA私钥存储在安全的位置（环境变量、密钥管理服务）
- ✅ 定期更换RSA密钥对
- ❌ 不要将私钥提交到版本控制系统
- ❌ 不要在前端代码中硬编码私钥

### 2. 传输安全
- ✅ 始终使用HTTPS协议传输加密后的数据
- ✅ RSA加密仅用于传输过程，存储仍使用BCrypt
- ✅ 实施TLS/SSL证书验证

### 3. 密码存储
- ✅ 使用BCrypt算法进行密码哈希
- ✅ 设置适当的BCrypt强度（默认10-12）
- ✅ 不要明文存储密码

### 4. 向后兼容
- `encrypted: false` 支持明文密码（仅用于开发/测试）
- 生产环境强制要求 `encrypted: true`

## 故障排查

### 问题1：加密失败
**症状**：前端加密返回 `false`

**解决方案**：
1. 检查公钥格式是否正确
2. 确认使用的是完整的公钥字符串
3. 检查JSEncrypt库是否正确加载

### 问题2：解密失败
**症状**：后端报错 "密码解密失败"

**解决方案**：
1. 检查RSA私钥是否正确配置
2. 确认前端使用的是正确的公钥
3. 检查加密数据的Base64格式

### 问题3：登录失败
**症状**：返回"用户名或密码错误"

**解决方案**：
1. 确认 `encrypted: true` 已添加到请求体
2. 检查加密密码是否完整传输
3. 查看后端日志确认解密是否成功

## 性能优化

### 1. 公钥缓存
前端获取公钥后应缓存，避免每次请求都获取。

### 2. 批量操作
如果需要批量加密密码，可复用 `JSEncrypt` 实例。

### 3. 密钥更新策略
实现密钥轮换机制，平滑过渡新旧密钥。

## 参考资料

- [JSEncrypt 文档](https://github.com/travist/jsencrypt)
- [Spring Security 加密最佳实践](https://docs.spring.io/spring-security/reference/)
- [OWASP 密码存储备忘单](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)

## 总结

RSA密码加密提供了以下安全保障：

1. ✅ 密码在传输过程中始终加密
2. ✅ 数据库中密码使用BCrypt哈希存储
3. ✅ 向后兼容，支持平滑迁移
4. ✅ 简单易用的API接口
5. ✅ 完善的前端集成示例

通过遵循本指南，您可以在前端安全地实现密码加密功能，保护用户密码的安全。
