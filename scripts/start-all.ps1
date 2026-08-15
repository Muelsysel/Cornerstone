# ============================================================
# Cornerstone 一键启动脚本
# 职责：检查依赖 → 并行启动后端 4 个服务 → 启动前端（dev 或容器）
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts/start-all.ps1              # 后端 + 前端 dev
#   powershell -ExecutionPolicy Bypass -File scripts/start-all.ps1 -NoFront     # 仅后端
#   powershell -ExecutionPolicy Bypass -File scripts/start-all.ps1 -FrontProd   # 前端用 docker 容器(8088)
#   powershell -ExecutionPolicy Bypass -File scripts/start-all.ps1 -Stop        # 停止脚本启动的服务
# 说明：Nacos/MySQL/Redis 依赖请先 `docker compose up -d`（或用 -NoDeps 跳过检查）。
# ============================================================
param(
    [string]$JavaHome = "C:\Dev\Lang\JAVA\JAVA17",
    [string]$MavenCmd = "D:\.develop\apache-maven-3.9.5\bin\mvn.cmd",
    [switch]$NoDeps,    # 跳过 docker 依赖端口检查
    [switch]$NoFront,   # 不启动前端
    [switch]$FrontProd, # 前端用 docker 容器（http://localhost:8088）而非 dev server
    [switch]$Stop       # 停止由本脚本启动的服务（按 logs/.pids 记录）
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$logDir = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$pidFile = Join-Path $logDir ".pids"

# ---------- Stop 模式 ----------
if ($Stop) {
    # 1) 按 .pids 记录停止（脚本启动的进程）
    if (Test-Path $pidFile) {
        Get-Content $pidFile | ForEach-Object {
            $procId = [int]$_
            if (Get-Process -Id $procId -ErrorAction SilentlyContinue) {
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                Write-Host "Stopped PID $procId"
            }
        }
        Remove-Item $pidFile -Force
    }
    # 2) 兜底：停止仍监听服务端口的进程（覆盖 .pids 记录不全/重启后 PID 漂移的情况）
    foreach ($port in 8080, 8081, 8082, 8083) {
        Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
            ForEach-Object {
                Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
                Write-Host "Stopped listener PID $($_.OwningProcess) on :$port"
            }
    }
    Write-Host "服务已停止（如仍有残留，可手动检查 8080-8083 端口占用）"
    exit 0
}

# ---------- 环境检查 ----------
$java = Join-Path $JavaHome "bin\java.exe"
if (-not (Test-Path $java)) { Write-Error "JDK not found: $java（可用 -JavaHome 指定）"; exit 1 }
if (-not (Test-Path $MavenCmd)) { Write-Error "Maven not found: $MavenCmd（可用 -MavenCmd 指定）"; exit 1 }
$env:JAVA_HOME = $JavaHome

# ---------- docker 依赖检查（Nacos 8848 / MySQL 3307 / Redis 6379） ----------
if (-not $NoDeps) {
    foreach ($port in 8848, 3307, 6379) {
        if (-not (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)) {
            Write-Warning "端口 $port 未监听——请先执行 docker compose up -d（或加 -NoDeps 跳过检查）"
        }
    }
}

# ---------- 依赖模块安装（common/api）----------
# spring-boot:run 单模块用本地仓库的依赖 jar：common/api 有新改动时须先 install，
# 否则服务启动报 ClassNotFoundException（如 RsaKeyUtils）。
Write-Host "安装依赖模块 cornerstone-common / cornerstone-api（-DskipTests）..."
& $MavenCmd -q install -pl cornerstone-common,cornerstone-api -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "依赖模块安装失败（exit $LASTEXITCODE），请检查 Maven/源码"
    exit 1
}

# ---------- 后端 4 服务并行启动 ----------
$modules = @('cornerstone-auth', 'cornerstone-system', 'cornerstone-demo', 'cornerstone-gateway')
$procs = @()
foreach ($m in $modules) {
    $out = Join-Path $logDir "$m.log"
    $err = Join-Path $logDir "$m.err"
    # spring-boot:run 一次只能跑一个模块，故每个模块一个独立进程
    $p = Start-Process -FilePath $MavenCmd `
        -ArgumentList @('-pl', $m, 'spring-boot:run', '-Dspring-boot.run.jvmArguments=-Xmx512m') `
        -PassThru -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err
    $procs += $p
    Write-Host "Starting $m (PID $($p.Id)) -> logs/$m.log"
}
$procs | ForEach-Object { $_.Id } | Set-Content $pidFile

# ---------- 等待端口就绪 ----------
$ports = @{ 'cornerstone-auth' = 8081; 'cornerstone-system' = 8082; 'cornerstone-demo' = 8083; 'cornerstone-gateway' = 8080 }
$deadline = (Get-Date).AddSeconds(180)
foreach ($m in $modules) {
    while ((Get-Date) -lt $deadline) {
        if (Get-NetTCPConnection -LocalPort $ports[$m] -State Listen -ErrorAction SilentlyContinue) { break }
        Start-Sleep -Seconds 3
    }
    $ready = [bool](Get-NetTCPConnection -LocalPort $ports[$m] -State Listen -ErrorAction SilentlyContinue)
    if ($ready) { Write-Host "[OK] $m :$($ports[$m])" }
    else {
        Write-Host "[FAIL] $m :$($ports[$m]) 未就绪，日志尾部："
        Get-Content (Join-Path $logDir "$m.log") -Tail 15 -ErrorAction SilentlyContinue
        Write-Host "提示：依赖是否已启动？可加 -NoDeps 跳过检查重试。"
        exit 1
    }
}

# ---------- 前端 ----------
if (-not $NoFront) {
    if ($FrontProd) {
        Write-Host "[OK] 前端容器启动中..."
        docker compose up -d --build frontend
        Write-Host "前端：http://localhost:8088 （admin / admin123）"
    } else {
        $frontOut = Join-Path $logDir "frontend.log"
        $p = Start-Process -FilePath "cmd.exe" `
            -ArgumentList @('/c', 'npm run dev') `
            -WorkingDirectory (Join-Path $root 'cornerstone-web') `
            -PassThru -WindowStyle Hidden -RedirectStandardOutput $frontOut -RedirectStandardError (Join-Path $logDir 'frontend.err')
        ($procs + $p) | ForEach-Object { $_.Id } | Set-Content $pidFile
        Write-Host "[OK] 前端 dev server 启动中（PID $($p.Id)）-> logs/frontend.log"
        Write-Host "前端：http://localhost:5173 （admin / admin123）"
    }
}

Write-Host ""
Write-Host "=== Cornerstone 启动完成 ==="
Write-Host "后端日志：logs/；停止：scripts/start-all.ps1 -Stop"
