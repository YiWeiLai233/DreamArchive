# DreamArchive 一键部署脚本 (Windows PowerShell)
# 用法: cd docker; .\deploy.ps1

$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $MyInvocation.MyCommand.Path)

Write-Host "╔══════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║   DreamArchive Docker 一键部署       ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

# 检查 Docker
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "错误: 未安装 Docker" -ForegroundColor Red
    exit 1
}

try {
    docker compose version | Out-Null
} catch {
    Write-Host "错误: 未安装 Docker Compose V2" -ForegroundColor Red
    exit 1
}

# 检查 .env 文件
if (-not (Test-Path .env)) {
    if (Test-Path .env.example) {
        Write-Host "未检测到 .env 文件，正在从模板创建..." -ForegroundColor Yellow
        Copy-Item .env.example .env
        Write-Host "请编辑 .env 文件填写真实配置，然后重新运行此脚本" -ForegroundColor Yellow
        exit 1
    } else {
        Write-Host "错误: 缺少 .env 和 .env.example 文件" -ForegroundColor Red
        exit 1
    }
}

# 读取 .env 并检查
$envContent = Get-Content .env -Raw
$requiredVars = @("MYSQL_ROOT_PASSWORD", "REDIS_PASSWORD", "MINIO_ROOT_USER", "MINIO_ROOT_PASSWORD", "MAIL_USERNAME", "MAIL_PASSWORD", "AUTH_SECRET")
$missing = $false
foreach ($var in $requiredVars) {
    $match = [regex]::Match($envContent, "(?m)^${var}=(.*)$")
    if (-not $match.Success -or $match.Groups[1].Value.Trim() -eq "" -or $match.Groups[1].Value.Trim().StartsWith("your_")) {
        Write-Host "错误: .env 中 $var 未填写或仍为模板值" -ForegroundColor Red
        $missing = $true
    }
}
if ($missing) {
    Write-Host "请编辑 .env 文件填写真实配置后重新运行" -ForegroundColor Yellow
    exit 1
}

# AUTH_SECRET 长度校验
$authMatch = [regex]::Match($envContent, "(?m)^AUTH_SECRET=(.*)$")
if ($authMatch.Success -and $authMatch.Groups[1].Value.Trim().Length -lt 32) {
    Write-Host "错误: AUTH_SECRET 至少需要 32 位，当前 $($authMatch.Groups[1].Value.Trim().Length) 位" -ForegroundColor Red
    exit 1
}

Write-Host "[1/3] 构建镜像..." -ForegroundColor Green
docker compose build --parallel

Write-Host "[2/3] 启动服务..." -ForegroundColor Green
docker compose up -d

Write-Host "[3/3] 等待服务就绪..." -ForegroundColor Green
Write-Host "  后端 → 等待启动 (约30-60秒)..." -ForegroundColor Cyan

$timeout = 120
$elapsed = 0
while ($elapsed -lt $timeout) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost/api/hello" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "  后端 → 就绪" -ForegroundColor Green
            break
        }
    } catch {}
    Start-Sleep -Seconds 3
    $elapsed += 3
}

if ($elapsed -ge $timeout) {
    Write-Host "  后端启动超时，请检查日志: docker compose logs backend" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "╔══════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║         部署完成！                    ║" -ForegroundColor Green
Write-Host "╠══════════════════════════════════════╣" -ForegroundColor Green
Write-Host "║  前端:      http://localhost          ║" -ForegroundColor Green
Write-Host "║  后端 API:  http://localhost/api/hello ║" -ForegroundColor Green
Write-Host "║  MinIO:     http://localhost:9001      ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "常用命令:" -ForegroundColor Cyan
Write-Host "  查看日志:  docker compose logs -f" -ForegroundColor Yellow
Write-Host "  停止服务:  docker compose down" -ForegroundColor Yellow
Write-Host "  重启后端:  docker compose restart backend" -ForegroundColor Yellow
Write-Host "  查看状态:  docker compose ps" -ForegroundColor Yellow
