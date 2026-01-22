# FinancialApp 快速部署脚本 (Windows PowerShell 版本)
# 用于在本地 Windows 环境准备部署文件

Write-Host "========================================" -ForegroundColor Green
Write-Host "  FinancialApp 部署准备脚本" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# 检查必要的工具
Write-Host "[1/5] 检查必要工具..." -ForegroundColor Yellow

# 检查 Git
try {
    $gitVersion = git --version
    Write-Host "✅ Git: $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Git 未安装，请先安装 Git" -ForegroundColor Red
    exit 1
}

# 检查 Gradle
try {
    $gradleVersion = .\gradlew.bat --version | Select-String "Gradle"
    Write-Host "✅ Gradle 已安装" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Gradle 将在 Docker 容器中构建" -ForegroundColor Yellow
}

# 检查 Docker
try {
    $dockerVersion = docker --version
    Write-Host "✅ Docker: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Docker 未安装，将跳过本地测试" -ForegroundColor Yellow
}

# 检查 Docker Compose
try {
    $composeVersion = docker-compose --version
    Write-Host "✅ Docker Compose: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Docker Compose 未安装" -ForegroundColor Yellow
}

# 构建项目
Write-Host ""
Write-Host "[2/5] 构建项目..." -ForegroundColor Yellow

Write-Host "执行 Gradle 构建..." -ForegroundColor Cyan
.\gradlew.bat clean build -x test

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 构建失败" -ForegroundColor Red
    exit 1
}

Write-Host "✅ 项目构建完成" -ForegroundColor Green

# 检查环境变量文件
Write-Host ""
Write-Host "[3/5] 检查环境变量..." -ForegroundColor Yellow

if (-not (Test-Path ".env")) {
    Write-Host "⚠️  未找到 .env 文件，从示例文件复制..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "✅ 已创建 .env 文件" -ForegroundColor Green
    Write-Host "⚠️  请编辑 .env 文件配置环境变量" -ForegroundColor Yellow
    Read-Host "按 Enter 继续..."
} else {
    Write-Host "✅ .env 文件已存在" -ForegroundColor Green
}

# 打包部署文件
Write-Host ""
Write-Host "[4/5] 打包部署文件..." -ForegroundColor Yellow

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$packageDir = ".\deploy-$timestamp"
$packageFile = ".\deploy-$timestamp.zip"

# 创建临时目录
New-Item -ItemType Directory -Path $packageDir -Force | Out-Null

# 复制必要文件
Write-Host "复制项目文件..." -ForegroundColor Cyan

Copy-Item ".\src" "$packageDir\src" -Recurse
Copy-Item ".\build.gradle.kts" "$packageDir\"
Copy-Item ".\settings.gradle.kts" "$packageDir\"
Copy-Item ".\gradle" "$packageDir\gradle" -Recurse
Copy-Item ".\gradlew" "$packageDir\"
Copy-Item ".\gradlew.bat" "$packageDir\"
Copy-Item ".\Dockerfile.prod" "$packageDir\Dockerfile"
Copy-Item ".\docker-compose.prod.yml" "$packageDir\docker-compose.yml"
Copy-Item ".\nginx" "$packageDir\nginx" -Recurse
Copy-Item ".\database" "$packageDir\database" -Recurse
Copy-Item ".\scripts\deploy.sh" "$packageDir\deploy.sh"
Copy-Item ".\scripts\quick-deploy.sh" "$packageDir\quick-deploy.sh"
Copy-Item ".\.env.example" "$packageDir\.env.example"
Copy-Item ".\DEPLOYMENT_GUIDE.md" "$packageDir\"
Copy-Item ".\README.md" "$packageDir\"

# 创建必要的空目录
New-Item -ItemType Directory -Path "$packageDir\uploads" -Force | Out-Null
New-Item -ItemType Directory -Path "$packageDir\logs\nginx" -Force | Out-Null
New-Item -ItemType Directory -Path "$packageDir\nginx\ssl" -Force | Out-Null
New-Item -ItemType Directory -Path "$packageDir\monitoring\prometheus" -Force | Out-Null
New-Item -ItemType Directory -Path "$packageDir\monitoring\grafana\dashboards" -Force | Out-Null
New-Item -ItemType Directory -Path "$packageDir\monitoring\grafana\datasources" -Force | Out-Null
New-Item -ItemType Directory -Path "$packageDir\mysql\conf.d" -Force | Out-Null

Write-Host "✅ 文件复制完成" -ForegroundColor Green

# 压缩文件
Write-Host "创建压缩包..." -ForegroundColor Cyan
Compress-Archive -Path "$packageDir\*" -DestinationPath $packageFile -Force

# 清理临时目录
Remove-Item -Path $packageDir -Recurse -Force

Write-Host "✅ 部署包创建完成: $packageFile" -ForegroundColor Green

# 显示部署说明
Write-Host ""
Write-Host "[5/5] 部署说明" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  📦 部署包准备完成" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "部署包位置: $packageFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "后续步骤:" -ForegroundColor Yellow
Write-Host "1. 将部署包上传到阿里云 ECS 服务器" -ForegroundColor White
Write-Host "2. 在服务器上解压: unzip deploy-$timestamp.zip" -ForegroundColor White
Write-Host "3. 进入目录: cd deploy-$timestamp" -ForegroundColor White
Write-Host "4. 编辑环境变量: nano .env" -ForegroundColor White
Write-Host "5. 执行部署: ./quick-deploy.sh" -ForegroundColor White
Write-Host ""
Write-Host "或者手动部署:" -ForegroundColor Yellow
Write-Host "1. 上传文件到服务器" -ForegroundColor White
Write-Host "2. 编辑 .env 文件配置环境变量" -ForegroundColor White
Write-Host "3. 执行: ./deploy.sh" -ForegroundColor White
Write-Host ""
Write-Host "详细说明请参考: DEPLOYMENT_GUIDE.md" -ForegroundColor Cyan
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ 准备完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Read-Host "按 Enter 退出"
