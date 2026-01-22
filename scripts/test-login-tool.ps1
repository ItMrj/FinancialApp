# 测试 login-test.html 功能的 PowerShell 脚本

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "  Login-Test.html 功能测试" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# 检查应用是否运行
Write-Host "[1/5] 检查应用是否运行..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health" -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ 应用正在运行" -ForegroundColor Green
    } else {
        Write-Host "❌ 应用未正常运行" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ 无法连接到应用，请确保应用已启动" -ForegroundColor Red
    Write-Host "   提示: 运行 './gradlew bootRun' 启动应用" -ForegroundColor Gray
    exit 1
}

# 检查静态资源访问
Write-Host "[2/5] 检查静态资源访问..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/login-test.html" -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ login-test.html 可以访问" -ForegroundColor Green
    } else {
        Write-Host "❌ login-test.html 访问失败" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ 无法访问 login-test.html" -ForegroundColor Red
}

# 检查 RSA 公钥接口
Write-Host "[3/5] 检查 RSA 公钥接口..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/rsa/public-key" -UseBasicParsing -TimeoutSec 5
    $data = $response.Content | ConvertFrom-Json
    if ($response.StatusCode -eq 200 -and $data.success) {
        Write-Host "✅ RSA 公钥接口正常" -ForegroundColor Green
        Write-Host "   公钥前缀: $($data.data.publicKey.Substring(0, 20))..." -ForegroundColor Gray
    } else {
        Write-Host "❌ RSA 公钥接口异常" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ RSA 公钥接口无法访问" -ForegroundColor Red
}

# 检查数据库连接
Write-Host "[4/5] 检查数据库连接..." -ForegroundColor Yellow
try {
    $result = & mysql -h localhost -uroot -proot -e "USE financial_app; SELECT COUNT(*) as user_count FROM users;" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 数据库连接正常" -ForegroundColor Green
        $count = $result | Select-String "\d+" | ForEach-Object { $_.Matches.Value }
        Write-Host "   当前用户数: $count" -ForegroundColor Gray
    } else {
        Write-Host "❌ 数据库连接失败" -ForegroundColor Red
    }
} catch {
    Write-Host "⚠️  无法检查数据库连接" -ForegroundColor Yellow
}

# 打开浏览器
Write-Host "[5/5] 打开浏览器..." -ForegroundColor Yellow
Write-Host ""
Write-Host "📌 正在打开 login-test.html..." -ForegroundColor Cyan

# 尝试使用默认浏览器打开
Start-Process "http://localhost:8080/login-test.html"

Write-Host ""
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "  测试完成！" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 使用说明:" -ForegroundColor Green
Write-Host "   1. 登录: 使用 admin/admin123 测试登录" -ForegroundColor Gray
Write-Host "   2. 注册: 点击'注册'标签创建新用户" -ForegroundColor Gray
Write-Host "   3. 用户中心: 登录后查看Token和用户信息" -ForegroundColor Gray
Write-Host ""
Write-Host "📚 详细文档: docs/login-test-usage-guide.md" -ForegroundColor Yellow
Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
