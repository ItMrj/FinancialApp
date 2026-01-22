# RSA 加密解密诊断脚本
# 用于诊断 "BadPaddingException: Decryption error" 问题

param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [switch]$Verbose
)

# 颜色输出函数
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

# 分隔线
function Write-Separator {
    Write-ColorOutput "=" * 80 -Color "Cyan"
}

# 测试 1：检查应用是否运行
function Test-ApplicationRunning {
    Write-Separator
    Write-ColorOutput "测试 1：检查应用是否运行" -Color "Yellow"
    
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/rsa/public-key" -Method GET -UseBasicParsing -TimeoutSec 5
        Write-ColorOutput "✅ 应用正在运行" -Color "Green"
        Write-ColorOutput "状态码: $($response.StatusCode)" -Color "Gray"
        return $true
    }
    catch {
        Write-ColorOutput "❌ 应用未运行或无法访问" -Color "Red"
        Write-ColorOutput "错误: $($_.Exception.Message)" -Color "Gray"
        return $false
    }
}

# 测试 2：获取公钥
function Get-PublicKey {
    Write-Separator
    Write-ColorOutput "测试 2：获取 RSA 公钥" -Color "Yellow"
    
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/rsa/public-key" -Method GET -UseBasicParsing
        $data = $response.Content | ConvertFrom-Json
        
        if ($data.success -and $data.data.publicKey) {
            $publicKey = $data.data.publicKey
            Write-ColorOutput "✅ 成功获取公钥" -Color "Green"
            Write-ColorOutput "算法: $($data.data.algorithm)" -Color "Gray"
            Write-ColorOutput "密钥大小: $($data.data.keySize) bits" -Color "Gray"
            Write-ColorOutput "公钥长度: $($publicKey.Length) 字符" -Color "Gray"
            
            if ($Verbose) {
                Write-ColorOutput "`n公钥内容:" -Color "Cyan"
                Write-Host $publicKey
            }
            
            return $publicKey
        }
        else {
            Write-ColorOutput "❌ 响应格式错误" -Color "Red"
            Write-ColorOutput $response.Content -Color "Gray"
            return $null
        }
    }
    catch {
        Write-ColorOutput "❌ 获取公钥失败" -Color "Red"
        Write-ColorOutput "错误: $($_.Exception.Message)" -Color "Gray"
        return $null
    }
}

# 测试 3：验证公钥格式
function Test-PublicKeyFormat {
    param([string]$PublicKey)
    
    Write-Separator
    Write-ColorOutput "测试 3：验证公钥格式" -Color "Yellow"
    
    $errors = @()
    
    if ([string]::IsNullOrWhiteSpace($PublicKey)) {
        $errors += "公钥为空"
    }
    else {
        # 检查是否包含 BEGIN 标记
        if (-not $PublicKey.StartsWith("-----BEGIN PUBLIC KEY-----")) {
            $errors += "缺少 BEGIN PUBLIC KEY 标记"
        }
        
        # 检查是否包含 END 标记
        if (-not $PublicKey.EndsWith("-----END PUBLIC KEY-----")) {
            $errors += "缺少 END PUBLIC KEY 标记"
        }
        
        # 检查长度
        if ($PublicKey.Length -lt 200) {
            $errors += "公钥长度过短（实际: $($PublicKey.Length)，期望: > 200）"
        }
        
        # 检查是否包含 Base64 字符
        $base64Part = $PublicKey -replace "-----BEGIN PUBLIC KEY-----", "" -replace "-----END PUBLIC KEY-----", "" -replace "`n", "" -replace "`r", ""
        if ($base64Part.Length -lt 100) {
            $errors += "Base64 内容过短"
        }
    }
    
    if ($errors.Count -eq 0) {
        Write-ColorOutput "✅ 公钥格式正确" -Color "Green"
        return $true
    }
    else {
        Write-ColorOutput "❌ 公钥格式错误：" -Color "Red"
        foreach ($error in $errors) {
            Write-ColorOutput "  - $error" -Color "Red"
        }
        return $false
    }
}

# 测试 4：测试登录（使用已知用户）
function Test-Login {
    param(
        [string]$Username = "admin",
        [string]$Password,
        [switch]$Encrypt
    )
    
    Write-Separator
    Write-ColorOutput "测试 4：测试登录" -Color "Yellow"
    
    $body = @{
        username = $Username
        password = $Password
        encrypted = $Encrypt
    } | ConvertTo-Json
    
    Write-ColorOutput "发送登录请求..." -Color "Cyan"
    
    if ($Verbose) {
        Write-ColorOutput "请求体:" -Color "Gray"
        Write-Host $body
    }
    
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/auth/login" -Method POST `
            -ContentType "application/json" `
            -Body $body `
            -UseBasicParsing
        
        Write-ColorOutput "✅ 登录请求成功发送" -Color "Green"
        Write-ColorOutput "状态码: $($response.StatusCode)" -Color "Gray"
        
        $data = $response.Content | ConvertFrom-Json
        if ($data.success) {
            Write-ColorOutput "✅ 登录成功！" -Color "Green"
            if ($Verbose -and $data.data) {
                Write-ColorOutput "Token 类型: $($data.data.tokenType)" -Color "Gray"
                Write-ColorOutput "Token 过期时间: $($data.data.expiresIn) ms" -Color "Gray"
            }
        }
        else {
            Write-ColorOutput "❌ 登录失败（返回 success=false）" -Color "Red"
            Write-ColorOutput $response.Content -Color "Gray"
        }
    }
    catch {
        Write-ColorOutput "❌ 登录请求失败" -Color "Red"
        Write-ColorOutput "错误: $($_.Exception.Message)" -Color "Red"
        
        if ($_.Exception.Response) {
            Write-ColorOutput "状态码: $($_.Exception.Response.StatusCode.value__)" -Color "Gray"
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-ColorOutput "响应内容: $responseBody" -Color "Gray"
        }
    }
}

# 测试 5：检查应用日志
function Check-ApplicationLogs {
    Write-Separator
    Write-ColorOutput "测试 5：检查应用日志" -Color "Yellow"
    Write-ColorOutput "请检查应用控制台输出，查找以下内容：" -Color "Cyan"
    Write-Host ""
    Write-ColorOutput "1. RSA 密钥生成日志：" -Color "Yellow"
    Write-Host "   临时RSA密钥对已生成"
    Write-Host "   公钥: ..."
    Write-Host "   私钥: ..."
    Write-Host ""
    Write-ColorOutput "2. 解密日志：" -Color "Yellow"
    Write-Host "   开始解密密码，密文长度: XXX"
    Write-Host "   密文前50个字符: ..."
    Write-Host ""
    Write-ColorOutput "3. 错误日志：" -Color "Yellow"
    Write-Host "   BadPaddingException: 密钥不匹配或密文被篡改"
}

# 主函数
function Main {
    Write-Separator
    Write-ColorOutput "RSA 加密解密诊断工具" -Color "Green"
    Write-ColorOutput "Base URL: $BaseUrl" -Color "Cyan"
    Write-Separator
    
    # 测试 1：检查应用是否运行
    if (-not (Test-ApplicationRunning)) {
        Write-ColorOutput "`n❌ 应用未运行，请先启动应用后再运行此脚本" -Color "Red"
        return
    }
    
    # 测试 2：获取公钥
    $publicKey = Get-PublicKey
    if (-not $publicKey) {
        Write-ColorOutput "`n❌ 无法获取公钥，请检查应用日志" -Color "Red"
        return
    }
    
    # 测试 3：验证公钥格式
    Test-PublicKeyFormat -PublicKey $publicKey
    
    # 测试 5：检查应用日志
    Check-ApplicationLogs
    
    Write-Separator
    Write-ColorOutput "诊断完成！" -Color "Green"
    Write-Separator
    
    Write-ColorOutput "`n💡 提示：" -Color "Yellow"
    Write-ColorOutput "1. 如果测试 2 和 3 都通过，说明后端 RSA 配置正确" -Color "White"
    Write-ColorOutput "2. 检查前端是否正确获取和使用公钥" -Color "White"
    Write-ColorOutput "3. 每次应用重启后，前端必须重新获取公钥" -Color "White"
    Write-ColorOutput "4. 如果使用临时密钥，建议将密钥配置到 application.yml" -Color "White"
    Write-Host ""
    
    Write-ColorOutput "📖 详细诊断指南请查看: docs/RSA_DIAGNOSTICS.md" -Color "Cyan"
}

# 运行主函数
Main
