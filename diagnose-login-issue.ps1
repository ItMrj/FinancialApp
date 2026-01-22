# 登录问题诊断脚本
# 用于诊断用户登录失败的问题

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  FinancialApp 登录问题诊断工具" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# 检查 MySQL 是否运行
Write-Host "[1] 检查 MySQL 服务状态..." -ForegroundColor Yellow
$mysqlProcess = Get-Process -Name mysqld -ErrorAction SilentlyContinue
if ($mysqlProcess) {
    Write-Host "✅ MySQL 服务正在运行 (PID: $($mysqlProcess.Id))" -ForegroundColor Green
} else {
    Write-Host "❌ MySQL 服务未运行，请先启动 MySQL" -ForegroundColor Red
    Write-Host ""
    Write-Host "提示：使用以下命令启动 MySQL（根据您的安装方式）："
    Write-Host "  - Windows 服务: net start MySQL80"
    Write-Host "  - Docker: docker start mysql"
    Write-Host ""
    exit 1
}
Write-Host ""

# 检查应用程序是否运行
Write-Host "[2] 检查 FinancialApp 服务状态..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health" -UseBasicParsing -TimeoutSec 2
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ FinancialApp 服务正在运行" -ForegroundColor Green
    } else {
        Write-Host "⚠️ FinancialApp 服务响应异常 (Status: $($response.StatusCode))" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ FinancialApp 服务未运行或无法访问" -ForegroundColor Red
    Write-Host "   错误: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "提示：使用以下命令启动应用程序："
    Write-Host "  ./gradlew bootRun"
    Write-Host ""
    exit 1
}
Write-Host ""

# 检查用户 abc 是否存在
Write-Host "[3] 检查用户 abc 是否存在..." -ForegroundColor Yellow
try {
    # 尝试使用 MySQL 命令行（如果可用）
    $mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
    if ($mysqlCmd) {
        $result = & mysql -h localhost -uroot -proot -e "USE financial_app; SELECT username, enabled, status, LEFT(password, 10) as password_prefix FROM users WHERE username='abc';" 2>&1

        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ 用户 abc 存在" -ForegroundColor Green
            Write-Host "   $result" -ForegroundColor Gray
        } else {
            Write-Host "⚠️ 无法查询用户 abc（可能 MySQL 命令不可用）" -ForegroundColor Yellow
        }
    } else {
        Write-Host "⚠️ MySQL 命令行工具不可用，请手动查询数据库" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 查询用户时出错: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 生成 BCrypt 密码
Write-Host "[4] BCrypt 密码生成工具..." -ForegroundColor Yellow
Write-Host "   运行以下命令生成 BCrypt 密码：" -ForegroundColor Gray
Write-Host "   ./gradlew generateBcryptPassword" -ForegroundColor Cyan
Write-Host ""

# 测试登录接口
Write-Host "[5] 测试登录接口..." -ForegroundColor Yellow
Write-Host "   测试用户: admin / admin123 (明文模式)" -ForegroundColor Gray
try {
    $body = @{
        username = "admin"
        password = "admin123"
        encrypted = $false
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
                                    -Method POST `
                                    -ContentType "application/json" `
                                    -Body $body `
                                    -ErrorAction Stop

    Write-Host "✅ 登录测试成功！" -ForegroundColor Green
    Write-Host "   响应: $($response.message)" -ForegroundColor Gray
} catch {
    Write-Host "❌ 登录测试失败" -ForegroundColor Red
    Write-Host "   错误: $($_.Exception.Message)" -ForegroundColor Red

    # 尝试读取响应内容
    try {
        $errorResponse = $_.ErrorDetails.Message | ConvertFrom-Json
        Write-Host "   响应: $($errorResponse.message)" -ForegroundColor Gray
    } catch {}
}
Write-Host ""

# 测试用户 abc 登录
Write-Host "[6] 测试用户 abc 登录..." -ForegroundColor Yellow
Write-Host "   提示：如果用户 abc 的密码是明文，请使用 encrypted=false" -ForegroundColor Gray
Write-Host ""
Write-Host "   测试命令：" -ForegroundColor Gray
Write-Host '   curl -X POST http://localhost:8080/api/auth/login \' -ForegroundColor Cyan
Write-Host '     -H "Content-Type: application/json" \' -ForegroundColor Cyan
Write-Host '     -d "{\"username\":\"abc\",\"password\":\"你的密码\",\"encrypted\":false}"' -ForegroundColor Cyan
Write-Host ""

# 总结和建议
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  诊断总结和建议" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "常见问题和解决方案：" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. 密码格式不匹配（最常见）" -ForegroundColor White
Write-Host "   问题：数据库中用户 abc 的密码不是 BCrypt 格式" -ForegroundColor Gray
Write-Host "   解决：" -ForegroundColor Gray
Write-Host "   - 运行: ./gradlew generateBcryptPassword" -ForegroundColor Cyan
Write-Host "   - 生成密码后，运行 SQL 更新数据库" -ForegroundColor Cyan
Write-Host "   - 参考: docs/quick-fix-abc-user.md" -ForegroundColor Cyan
Write-Host ""

Write-Host "2. encrypted 标志不正确" -ForegroundColor White
Write-Host "   问题：前端发送的 encrypted 标志与密码类型不匹配" -ForegroundColor Gray
Write-Host "   解决：" -ForegroundColor Gray
Write-Host "   - 如果数据库密码是明文，使用 encrypted=false" -ForegroundColor Cyan
Write-Host "   - 如果数据库密码是 BCrypt，使用 encrypted=true（需要先获取公钥加密）" -ForegroundColor Cyan
Write-Host "   - 使用 login-test.html 测试两种模式" -ForegroundColor Cyan
Write-Host ""

Write-Host "3. 用户状态异常" -ForegroundColor White
Write-Host "   问题：enabled=false 或 status != ACTIVE" -ForegroundColor Gray
Write-Host "   解决：" -ForegroundColor Gray
Write-Host "   - 运行 SQL: UPDATE users SET enabled=TRUE, status='ACTIVE' WHERE username='abc';" -ForegroundColor Cyan
Write-Host ""

Write-Host "相关文件：" -ForegroundColor Yellow
Write-Host "  - docs/quick-fix-abc-user.md          快速修复指南" -ForegroundColor Gray
Write-Host "  - docs/login-failure-troubleshooting.md 完整诊断指南" -ForegroundColor Gray
Write-Host "  - database/check-and-fix-user-abc.sql    数据库检查脚本" -ForegroundColor Gray
Write-Host "  - src/main/resources/static/login-test.html  登录测试工具" -ForegroundColor Gray
Write-Host ""

Write-Host "使用 login-test.html：" -ForegroundColor Yellow
Write-Host "  1. 访问: http://localhost:8080/login-test.html" -ForegroundColor Gray
Write-Host "  2. 输入用户名和密码" -ForegroundColor Gray
Write-Host "  3. 明文模式：取消勾选'加密密码'，直接登录" -ForegroundColor Gray
Write-Host "  4. 加密模式：勾选'加密密码'，获取公钥后登录" -ForegroundColor Gray
Write-Host ""

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  诊断完成" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
