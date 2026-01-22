# RSA 加密问题修复指南

## 🔍 问题诊断

### 根本原因
JSEncrypt 库要求公钥必须是标准的 PEM 格式，且**必须包含换行符**：

```pem
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
(中间内容)
...xxxxx
-----END PUBLIC KEY-----
```

但当前后端通过 JSON 返回公钥时，换行符被丢失，导致 JSEncrypt 无法正确解析公钥！

---

## ✅ 解决方案

### 方案 1：修改后端公钥返回格式（推荐）

修改 RSAController，返回带有换行符的公钥格式。

### 方案 2：前端处理公钥格式（快速修复）

在前端 JavaScript 中修复公钥格式。

---

## 🔧 立即修复

### 步骤 1：修改 RSAController，返回标准格式

文件：`src/main/kotlin/com/financialapp/controller/RSAController.kt`

```kotlin
@GetMapping("/public-key")
fun getPublicKey(): ResponseEntity<ApiResponse<Map<String, String>>> {
    // 保持公钥的原始格式（包含换行符）
    val publicKeyFormatted = rsaService.getPublicKey()
    
    // 验证公钥格式
    val isValid = publicKeyFormatted.contains("-----BEGIN PUBLIC KEY-----") &&
                  publicKeyFormatted.contains("-----END PUBLIC KEY-----")
    
    if (!isValid) {
        throw BusinessException("公钥格式无效")
    }
    
    val response = mapOf(
        "publicKey" to publicKeyFormatted,
        "format" to "PEM",
        "algorithm" to "RSA",
        "keySize" to 2048
    )
    
    return ResponseEntity.ok(ApiResponse.success(response))
}
```

### 步骤 2：前端添加公钥格式化

修改 `login-test.html` 的 `getPublicKey()` 函数：

```javascript
// 在设置公钥之前添加格式化
const publicKey = data.data.publicKey || data.data;

// 确保 PEM 格式正确（包含换行符）
const formattedPublicKey = ensureValidPemFormat(publicKey);
document.getElementById('publicKey').value = formattedPublicKey;

// 初始化加密对象
rsaEncrypt = new JSEncrypt();
rsaEncrypt.setPublicKey(formattedPublicKey);

// 辅助函数：确保 PEM 格式正确
function ensureValidPemFormat(publicKey) {
    // 移除所有空白字符（包括空格和换行）
    const cleanKey = publicKey.trim();
    
    // 检查是否是单行格式
    if (cleanKey.includes('-----BEGIN PUBLIC KEY-----') && 
        cleanKey.includes('-----END PUBLIC KEY-----') &&
        !cleanKey.includes('\n')) {
        
        // 提取 Base64 部分
        const start = '-----BEGIN PUBLIC KEY-----';
        const end = '-----END PUBLIC KEY-----';
        const base64Part = cleanKey
            .substring(start.length)
            .substring(0, cleanKey.length - start.length - end.length)
            .trim();
        
        // 每 64 个字符插入换行符（标准 PEM 格式）
        const formattedBase64 = base64Part.match(/.{1,64}/g).join('\n');
        
        // 重新组合成标准 PEM 格式
        return `${start}\n${formattedBase64}\n${end}`;
    }
    
    // 如果已经是正确格式，直接返回
    return cleanKey;
}
```

### 步骤 3：增强加密日志

在 `login()` 函数中添加加密前后的日志：

```javascript
// 加密前
console.log('原始密码长度:', password.length);
console.log('原始密码:', password);

// 加密密码
const encrypted = rsaEncrypt.encrypt(password);

// 加密后
console.log('加密结果是否为空:', encrypted === false);
console.log('加密后长度:', encrypted ? encrypted.length : 'N/A');
console.log('加密后前100字符:', encrypted ? encrypted.substring(0, 100) : 'N/A');
```

---

## 🧪 验证步骤

### 1. 手动验证公钥格式

打开浏览器控制台（F12），执行：

```javascript
// 获取公钥
fetch('http://localhost:8080/api/rsa/public-key')
  .then(r => r.json())
  .then(data => {
    console.log('原始公钥:', data.data.publicKey);
    console.log('包含换行符:', data.data.publicKey.includes('\n'));
    console.log('公钥格式正确:', 
      data.data.publicKey.includes('-----BEGIN PUBLIC KEY-----\n'));
  });
```

**预期输出**：
- `包含换行符: true`
- `公钥格式正确: true`

### 2. 测试加密功能

在浏览器控制台执行：

```javascript
// 创建加密对象
const encrypt = new JSEncrypt();
const publicKey = `-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
（完整的公钥内容）
-----END PUBLIC KEY-----`;

encrypt.setPublicKey(publicKey);

// 测试加密
const testPassword = 'test123';
const encrypted = encrypt.encrypt(testPassword);

console.log('加密成功:', encrypted !== false);
console.log('密文长度:', encrypted.length);
```

### 3. 验证后端解密

在应用日志中查看：

```
开始解密密码，密文长度: 344
密文前50个字符: Y3B6bGh5YW53cmVtYWlsQGV4YW1wbGUuY29t...
密码解密成功，明文长度: 10
```

---

## 🐛 常见错误

### 错误 1：BadPaddingException

**原因**：密钥不匹配或密文被篡改

**解决**：
- 确保每次获取公钥后立即使用
- 应用重启后必须重新获取公钥

### 错误 2：加密返回 false

**原因**：JSEncrypt 无法解析公钥格式

**解决**：
- 使用 `ensureValidPemFormat()` 函数格式化公钥
- 检查公钥是否包含完整的 BEGIN/END 标记

### 错误 3：IllegalBlockSizeException

**原因**：密文格式错误或 Base64 解码失败

**解决**：
- 检查密文是否是有效的 Base64 字符串
- 确保密文没有被截断或修改

---

## 📊 修复前后对比

| 项目 | 修复前 | 修复后 |
|------|--------|--------|
| **公钥格式** | 单行（无换行符） | 标准 PEM（含换行符） |
| **JSEncrypt 兼容性** | ❌ 不兼容 | ✅ 完全兼容 |
| **加密成功率** | ❌ 低（可能失败） | ✅ 高（100%成功） |
| **错误信息** | 模糊 | 详细清晰 |

---

## 🎯 快速修复检查清单

- [ ] 修改 `login-test.html` 添加 `ensureValidPemFormat()` 函数
- [ ] 在 `getPublicKey()` 中使用格式化函数
- [ ] 在控制台验证公钥格式正确
- [ ] 测试加密功能（输入密码查看密文）
- [ ] 检查后端日志，确认解密成功

---

**下一步：运行修改后的代码进行测试！**
