# Cornerstone end-to-end auth chain verification.
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/verify-chain.ps1 -UseRunning
#      复用已在运行的服务（如 scripts/start-all.ps1 启动的），直接跑链路断言；
#   powershell -ExecutionPolicy Bypass -File scripts/verify-chain.ps1
#      自行启动（需先 mvn -DskipTests package），验证后自动停止。
# Requires docker dependencies up: `docker compose up -d`
param(
    [string]$JavaHome = "C:\Dev\Lang\JAVA\JAVA17",
    [int]$WaitSeconds = 120,
    [switch]$UseRunning
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$services = @(
    @{ Name = "auth";    Port = 8081; Jar = "cornerstone-auth\target\cornerstone-auth-1.0.0-SNAPSHOT.jar" },
    @{ Name = "system";  Port = 8082; Jar = "cornerstone-system\target\cornerstone-system-1.0.0-SNAPSHOT.jar" },
    @{ Name = "demo";    Port = 8083; Jar = "cornerstone-demo\target\cornerstone-demo-1.0.0-SNAPSHOT.jar" },
    @{ Name = "gateway"; Port = 8080; Jar = "cornerstone-gateway\target\cornerstone-gateway-1.0.0-SNAPSHOT.jar" }
)

$procs = @()
$logDir = Join-Path $env:TEMP "cornerstone-verify"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if ($UseRunning) {
    foreach ($s in $services) {
        $ready = [bool](Get-NetTCPConnection -LocalPort $s.Port -State Listen -ErrorAction SilentlyContinue)
        if (-not $ready) {
            Write-Error "UseRunning 模式要求 $($s.Name) :$($s.Port) 已在监听（请先执行 scripts/start-all.ps1）"
            exit 1
        }
        Write-Host "UseRunning: 复用运行中的 $($s.Name) :$($s.Port)"
    }
} else {
    $java = Join-Path $JavaHome "bin\java.exe"
    if (-not (Test-Path $java)) { Write-Error "JDK not found: $java"; exit 1 }
    foreach ($s in $services) {
        if (-not (Test-Path $s.Jar)) {
            Write-Error "Missing jar: $($s.Jar) (run 'mvn -DskipTests package' first)"
            exit 1
        }
        $out = Join-Path $logDir "$($s.Name).log"
        $err = Join-Path $logDir "$($s.Name).err"
        $p = Start-Process -FilePath $java -ArgumentList @('-jar', (Resolve-Path $s.Jar)) `
            -PassThru -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err
        $procs += $p
        Write-Host "Starting $($s.Name) (PID $($p.Id))"
    }
}

try {
    # Wait for ports
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    foreach ($s in $services) {
        while ((Get-Date) -lt $deadline) {
            if (Get-NetTCPConnection -LocalPort $s.Port -State Listen -ErrorAction SilentlyContinue) { break }
            Start-Sleep -Seconds 3
        }
        $ready = [bool](Get-NetTCPConnection -LocalPort $s.Port -State Listen -ErrorAction SilentlyContinue)
        Write-Host "$($s.Name) :$($s.Port) ready=$ready"
        if (-not $ready) {
            Write-Host "--- $($s.Name) log tail ---"
            Get-Content (Join-Path $logDir "$($s.Name).log") -Tail 15 -ErrorAction SilentlyContinue
            exit 1
        }
    }

    $script:fail = 0
    function Assert([string]$Desc, [string]$Actual, [string]$Expected) {
        if ($Actual -eq $Expected) { Write-Host "PASS  $Desc ($Actual)" }
        else { Write-Host "FAIL  $Desc (expected $Expected, got $Actual)"; $script:fail = 1 }
    }

    # 1. Get token through gateway (client_credentials, Basic auth)
    $body = 'grant_type=client_credentials&scope=read'
    $tokenResp = curl.exe -s --max-time 20 -u 'cornerstone-client:cornerstone-secret' `
        -X POST 'http://localhost:8080/auth/oauth2/token' `
        -H 'Content-Type: application/x-www-form-urlencoded' -d $body
    $token = $null
    try { $token = ($tokenResp | ConvertFrom-Json).access_token } catch {}
    Assert 'Get token via gateway' $(if ($token) { 'OK' } else { 'FAIL' }) 'OK'

    # 2. Public endpoint without token -> 200
    $pageUrl = 'http://localhost:8080/demo/announcement/page?pageNum=1&pageSize=10'
    $code = curl.exe -s -o NUL -w '%{http_code}' --max-time 20 $pageUrl
    Assert 'Public endpoint no token 200' $code '200'

    # 3. Protected endpoint without token -> 401
    $code = curl.exe -s -o NUL -w '%{http_code}' --max-time 20 'http://localhost:8080/system/user/1'
    Assert 'Protected no token 401' $code '401'

    # 4. Protected endpoint with token -> 200
    $code = curl.exe -s -o NUL -w '%{http_code}' --max-time 20 -H "Authorization: Bearer $token" 'http://localhost:8080/system/user/1'
    Assert 'Protected with token 200' $code '200'

    # 5. Invalid token -> 401
    $code = curl.exe -s -o NUL -w '%{http_code}' --max-time 20 -H 'Authorization: Bearer invalid.token.here' 'http://localhost:8080/system/user/1'
    Assert 'Invalid token 401' $code '401'

    if ($script:fail -eq 0) {
        Write-Host ''
        Write-Host '=== End-to-end verification PASSED ==='
    } else {
        Write-Host ''
        Write-Host '=== Verification FAILED ==='
        exit 1
    }
}
finally {
    if ($UseRunning) {
        Write-Host 'UseRunning 模式：服务保持运行（由 start-all 管理，可用 scripts/start-all.ps1 -Stop 停止）'
    } else {
        foreach ($p in $procs) {
            if (-not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
        }
        Write-Host 'All service processes stopped'
    }
}
