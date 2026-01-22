# RSA 加密修复验证脚本
# 用于验证 RSA 加密修复是否生效

param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [switch]$Verbose
)

function Write-Verbose-Output {
    param([string]$Message)
    if ($Verbose) {
        Write-Host "`n[VERBOSE] $Message" -ForegroundColor Cyan
    }
}

function Write-Test {
    param([string]$Name)
    Write-Host "`n" -NoNewline
    Write-Host "[$Name]" -ForegroundColor Yellow -NoNewline
    Write-Host " " -NoNewline
}

function Write-Pass {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  $Message" -ForegroundColor Gray
}

# 测试 1：检查应用是否运行
Write-Test "测试 1：检查应用状态"
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/rsa/public-key" -Method GET -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Pass "应用运行正常"
    } else {
        Write-Fail "应用返回状态码: $($response.StatusCode)"
        exit 1
    }
} catch {
    Write-Fail "无法连接到应用，请确保应用正在运行"
    Write-Info "错误: $($_.Exception.Message)"
    exit 1
}

# 测试 2：获取公钥并检查格式
Write-Test "测试 2：公钥格式验证"
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/rsa/public-key" -Method GET -UseBasicParsing
    $data = $response.Content | ConvertFrom-Json
    $publicKey = $data.data.publicKey

    Write-Info "公钥长度: $($publicKey.Length) 字符"

    if ($publicKey -match "-----BEGIN PUBLIC KEY-----") {
        Write-Pass "包含 BEGIN 标记"
    } else {
        Write-Fail "缺少 BEGIN 标记"
    }

    if ($publicKey -match "-----END PUBLIC KEY-----") {
        Write-Pass "包含 END 标记"
    } else {
        Write-Fail "缺少 END 标记"
    }

    # 检查换行符
    if ($publicKey -match "`n") {
        Write-Pass "包含换行符（标准 PEM 格式）"
    } else {
        Write-Fail "缺少换行符（需要前端格式化）"
        Write-Info "提示：前端会自动添加换行符"
    }

    Write-Verbose-Output "公钥前 100 字符: $($publicKey.Substring(0, [Math]::Min(100, $publicKey.Length)))"
} catch {
    Write-Fail "解析公钥失败"
    Write-Info "错误: $($_.Exception.Message)"
}

# 测试 3：PEM 端点测试
Write-Test "测试 3：PEM 格式端点"
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/rsa/public-key/pem" -Method GET -UseBasicParsing
    $publicKeyPEM = $response.Content

    Write-Info "PEM 长度: $($publicKeyPEM.Length) 字符"

    if ($publicKeyPEM -match "`n") {
        Write-Pass "PEM 格式包含换行符"
    } else {
        Write-Fail "PEM 格式缺少换行符"
    }
} catch {
    Write-Fail "PEM 端点访问失败"
    Write-Info "错误: $($_.Exception.Message)"
}

# 测试 4：加密功能测试
Write-Test "测试 4：后端加密测试"
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/rsa/test-encryption" -Method GET -UseBasicParsing
    $data = $response.Content | ConvertFrom-Json

    if ($data.success) {
        Write-Pass "后端加密测试成功"
        Write-Info "加密密文长度: $($data.data.encryptedLength) 字符"
        Write-Info "公钥格式: $($data.data.publicKeyFormat)"
    } else {
        Write-Fail "后端加密测试失败"
    }
} catch {
    Write-Fail "加密测试请求失败"
    Write-Info "错误: $($_.Exception.Message)"
}

# 测试 5：前端格式化功能（模拟）
Write-Test "测试 5：前端格式化模拟"
try {
    # 获取公钥
    $response = Invoke-WebRequest -Uri "$BaseUrl/rsa/public-key" -Method GET -UseBasicParsing
    $data = $response.Content | ConvertFrom-Json
    $publicKey = $data.data.publicKey

    # 模拟前端格式化（如果公钥是单行）
    if ($publicKey -notmatch "`n") {
        Write-Info "检测到单行格式，模拟前端格式化..."

        # 提取 Base64 部分
        $base64Part = $publicKey -replace "-----BEGIN PUBLIC KEY-----", "" -replace "-----END PUBLIC KEY-----", "" -replace "\s", ""

        # 每 64 个字符插入换行符
        $formattedBase64 = ($base64Part -split '(.{64})' | Where-Object { $_ }) -join "`n"

        # 重新组合
        $formattedKey = "-----BEGIN PUBLIC KEY-----`n$formattedBase64`n-----END PUBLIC KEY-----"

        Write-Info "格式化后公钥长度: $($formattedKey.Length) 字符"

        if ($formattedKey -match "`n") {
            Write-Pass "格式化成功，包含换行符"
        } else {
            Write-Fail "格式化失败"
        }
    } else {
        Write-Pass "公钥已包含换行符，无需格式化"
    }
} catch {
    Write-Fail "前端格式化测试失败"
    Write-Info "错误: $($_.Exception.Message)"
}

# 测试 6：登录端点连通性测试
Write-Test "测试 6：登录端点连通性"
try {
    # 发送一个测试请求（预期会失败，但可以测试端点是否可达）
    $testBody = @{
        username = "test_user_12345"
        password = "test_password_123"
        encrypted = $false
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Uri "$BaseUrl/auth/login" -Method POST -Body $testBody -ContentType "application/json" -UseBasicParsing -ErrorAction SilentlyContinue

    if ($response.StatusCode -eq 401 -or $response.StatusCode -eq 400) {
        Write-Pass "登录端点可达（预期返回错误：用户不存在）"
    } elseif ($response.StatusCode -eq 200) {
        Write-Pass "登录端点可达（返回成功）"
    } else {
        Write-Info "登录端点返回状态码: $($response.StatusCode)"
    }
} catch {
    Write-Info "登录端点测试：$($_.Exception.Message)"
}

# 总结
Write-Host "`n" -NoNewline
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试完成！" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "下一步操作：" -ForegroundColor Yellow
Write-Host "1. 打开浏览器访问: http://localhost:8080/login-test.html" -ForegroundColor White
Write-Host "2. 打开浏览器控制台（F12）" -ForegroundColor White
Write-Host "3. 观察控制台日志，验证公钥格式化是否成功" -ForegroundColor White
Write-Host "4. 尝试登录，查看是否还有 BadPaddingException 错误" -ForegroundColor White
Write-Host "`n如果仍有问题，请查看应用日志中的详细错误信息。" -ForegroundColor Gray
