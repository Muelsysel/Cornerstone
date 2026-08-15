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

    # JSON body 统一走临时文件（PS 5.1 向 curl.exe 传含双引号的 -d 参数会破坏 JSON）
    $tmpJson = Join-Path $env:TEMP "cornerstone-verify-body.json"
    function Write-JsonBody([string]$Content) {
        [System.IO.File]::WriteAllText($tmpJson, $Content, (New-Object System.Text.UTF8Encoding($false)))
    }

    # 1b. 用户登录令牌（管理类断言需要带用户身份与权限的 JWT；client_credentials 令牌无权限点）
    Write-JsonBody '{"username":"admin","password":"admin123"}'
    $adminLogin = curl.exe -s --max-time 20 -X POST 'http://localhost:8080/auth/login' `
        -H 'Content-Type: application/json' --data-binary "@$tmpJson"
    $adminToken = $null
    try { $adminToken = ($adminLogin | ConvertFrom-Json).data.access_token } catch {}
    Assert 'Login admin user' $(if ($adminToken) { 'OK' } else { 'FAIL' }) 'OK'

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

    # 6. 分页参数契约：pageNum/pageSize 必须直达后端（曾回归为 current/size 导致翻页失效）
    $pageResp = curl.exe -s --max-time 20 -H "Authorization: Bearer $adminToken" `
        'http://localhost:8080/system/user/page?pageNum=2&pageSize=5'
    $current = $null
    try { $current = ($pageResp | ConvertFrom-Json).data.current } catch {}
    Assert 'Pagination passthrough pageNum/pageSize (current=2)' $(if ($current -eq 2) { 'OK' } else { 'FAIL' }) 'OK'

    # 6b. 菜单树查询（admin 需能获取完整菜单树）
    $menuResp = curl.exe -s --max-time 20 -H "Authorization: Bearer $adminToken" `
        'http://localhost:8080/system/menu/tree'
    $menuOk = 'FAIL'
    try {
        $menuArr = ($menuResp | ConvertFrom-Json).data
        if ($menuArr -is [array] -and $menuArr.Count -gt 0) { $menuOk = 'OK' }
    } catch {}
    Assert 'Menu tree accessible (admin)' $menuOk 'OK'

    # 6c. 部门树查询（admin 需能获取完整部门树）
    $deptResp = curl.exe -s --max-time 20 -H "Authorization: Bearer $adminToken" `
        'http://localhost:8080/system/dept/tree'
    $deptOk = 'FAIL'
    try {
        $deptArr = ($deptResp | ConvertFrom-Json).data
        if ($deptArr -is [array] -and $deptArr.Count -gt 0) { $deptOk = 'OK' }
    } catch {}
    Assert 'Dept tree accessible (admin)' $deptOk 'OK'

    # 6d. 审计契约：登录日志含 admin 记录（登录成功已投递审计，回归防丢失）
    $logResp = curl.exe -s --max-time 20 -H "Authorization: Bearer $adminToken" `
        'http://localhost:8080/system/loginlog/page?pageNum=1&pageSize=20&username=admin'
    $logOk = 'FAIL'
    try {
        $logRecs = ($logResp | ConvertFrom-Json).data.records
        if ($logRecs -is [array] -and $logRecs.Count -gt 0) { $logOk = 'OK' }
    } catch {}
    Assert 'Login log contains admin record (audit)' $logOk 'OK'

    # 7. 公告编辑契约：POST 创建草稿 -> PUT /{id} 更新（曾因 PUT 缺 id 路径 100% 失败）
    $ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $annTitle = "verify-chain-$ts"
    Write-JsonBody "{`"title`":`"$annTitle`",`"content`":`"auto-check`"}"
    $annResp = curl.exe -s --max-time 20 -X POST 'http://localhost:8080/demo/announcement' `
        -H "Authorization: Bearer $adminToken" -H 'Content-Type: application/json' --data-binary "@$tmpJson"
    $annCode = $null
    try { $annCode = ($annResp | ConvertFrom-Json).code } catch {}
    Assert 'Create announcement (POST) 200' $(if ($annCode -eq 200) { 'OK' } else { 'FAIL' }) 'OK'
    $draftPage = curl.exe -s --max-time 20 -H "Authorization: Bearer $adminToken" `
        "http://localhost:8080/demo/announcement/page?pageNum=1&pageSize=20&title=$annTitle"
    $draftId = $null
    try { $draftId = (($draftPage | ConvertFrom-Json).data.records | Where-Object { $_.title -eq $annTitle } | Select-Object -First 1).id } catch {}
    if ($draftId) {
        Write-JsonBody "{`"id`":$draftId,`"title`":`"$annTitle-2`",`"content`":`"updated`"}"
        $updResp = curl.exe -s --max-time 20 -X PUT "http://localhost:8080/demo/announcement/$draftId" `
            -H "Authorization: Bearer $adminToken" -H 'Content-Type: application/json' --data-binary "@$tmpJson"
        $updCode = $null
        try { $updCode = ($updResp | ConvertFrom-Json).code } catch {}
        Assert 'Update announcement PUT /{id} 200' $(if ($updCode -eq 200) { 'OK' } else { 'FAIL' }) 'OK'

        # 7b. 状态机契约：发布（草稿→已发布）→ 下线（已发布→已下线）
        #     状态驱动断言：POST 操作后以详情查询为准（最多 3 次尝试），不依赖操作响应——
        #     本机 curl 偶发连接丢失时，只要操作实际生效（状态变化）即判定成功，避免假失败
        $pubOk = 'FAIL'
        for ($attempt = 1; $attempt -le 3 -and $pubOk -ne 'OK'; $attempt++) {
            curl.exe -s -o NUL --max-time 20 -X POST "http://localhost:8080/demo/announcement/$draftId/publish" `
                -H "Authorization: Bearer $adminToken"
            Start-Sleep -Milliseconds 300
            $stResp = curl.exe -s --max-time 20 -H "Authorization: Bearer $adminToken" `
                "http://localhost:8080/demo/announcement/$draftId"
            try {
                $stRec = ($stResp | ConvertFrom-Json).data
                if ($stRec -and $stRec.status -eq 1) { $pubOk = 'OK' }
            } catch {}
        }
        Assert 'Publish announcement POST /{id}/publish 200' $pubOk 'OK'
        $offOk = 'FAIL'
        for ($attempt = 1; $attempt -le 3 -and $offOk -ne 'OK'; $attempt++) {
            curl.exe -s -o NUL --max-time 20 -X POST "http://localhost:8080/demo/announcement/$draftId/offline" `
                -H "Authorization: Bearer $adminToken"
            Start-Sleep -Milliseconds 300
            $stResp = curl.exe -s --max-time 20 -H "Authorization: Bearer $adminToken" `
                "http://localhost:8080/demo/announcement/$draftId"
            try {
                $stRec = ($stResp | ConvertFrom-Json).data
                if ($stRec -and $stRec.status -eq 2) { $offOk = 'OK' }
            } catch {}
        }
        Assert 'Offline announcement POST /{id}/offline 200' $offOk 'OK'

        # 8. 隐私契约：游客访问非已发布（草稿/已下线）详情 -> 业务码非 200（按不存在处理，防泄露）
        $guestDetail = curl.exe -s --max-time 20 "http://localhost:8080/demo/announcement/$draftId"
        $guestCode = $null
        try { $guestCode = ($guestDetail | ConvertFrom-Json).code } catch {}
        Assert 'Guest cannot read non-published detail (code != 200)' $(if ($guestCode -ne 200) { 'OK' } else { 'FAIL' }) 'OK'

        # 清理临时草稿
        curl.exe -s -o NUL --max-time 20 -X DELETE "http://localhost:8080/demo/announcement/$draftId" `
            -H "Authorization: Bearer $adminToken" | Out-Null
    } else {
        Assert 'Locate created draft id' 'FAIL' 'OK'
    }

    # 9. IDOR：普通用户无权查看他人资料（曾可越权传任意 userId 查他人信息）
    #     重试一次：本机 Windows curl 偶发 stdout 捕获异常（连接建立但输出丢失），重试可消除偶发假失败
    $testToken = $null
    for ($attempt = 1; $attempt -le 2 -and -not $testToken; $attempt++) {
        Write-JsonBody '{"username":"test","password":"admin123"}'
        $testLogin = curl.exe -s --max-time 20 -X POST 'http://localhost:8080/auth/login' `
            -H 'Content-Type: application/json' --data-binary "@$tmpJson"
        try { $testToken = ($testLogin | ConvertFrom-Json).data.access_token } catch {}
        if (-not $testToken -and $attempt -lt 2) { Start-Sleep -Seconds 1 }
    }
    if ($testToken) {
        # IDOR 请求同样重试
        $infoCode = $null
        for ($attempt = 1; $attempt -le 2 -and $infoCode -ne 403; $attempt++) {
            $infoResp = curl.exe -s --max-time 20 -H "Authorization: Bearer $testToken" `
                'http://localhost:8080/system/user/info?userId=1'
            try { $infoCode = ($infoResp | ConvertFrom-Json).code } catch {}
            if ($infoCode -ne 403 -and $attempt -lt 2) { Start-Sleep -Seconds 1 }
        }
        if ($infoCode -ne 403) {
            Write-Host "  INFO-RAW[length=$($infoResp.Length)]: $infoResp"
        }
        Assert 'IDOR blocked: cross-user info 403' $(if ($infoCode -eq 403) { 'OK' } else { 'FAIL' }) 'OK'

        # 9b. IDOR 正向：test 查自己（userId=2）应成功（仅限本人语义的合法访问）
        $selfCode = $null
        $selfResp = curl.exe -s --max-time 20 -H "Authorization: Bearer $testToken" `
            'http://localhost:8080/system/user/info?userId=2'
        try { $selfCode = ($selfResp | ConvertFrom-Json).code } catch {}
        Assert 'IDOR allowed: self info 200' $(if ($selfCode -eq 200) { 'OK' } else { 'FAIL' }) 'OK'
    } else {
        Assert 'Login test user' 'FAIL' 'OK'
    }

    # 10. 登录锁定：连续失败 5 次后响应变化（用临时用户名避免污染真实账号；锁定消息与普通失败不同）
    #     注意：登录请求受网关登录限流（5/s 桶 10）保护——间隔 300ms 避免 verify 自身触发 429
    $lockUser = "verify-lock-$ts"
    $firstResp = $null
    $sixthResp = $null
    for ($i = 1; $i -le 6; $i++) {
        Write-JsonBody "{`"username`":`"$lockUser`",`"password`":`"wrong`"}"
        $r = $null
        # 失败登录幂等：响应丢失（curl 偶发）时重试该次，不影响锁定计数语义
        for ($attempt = 1; $attempt -le 3 -and -not $r; $attempt++) {
            $r = curl.exe -s --max-time 20 -X POST 'http://localhost:8080/auth/login' `
                -H 'Content-Type: application/json' --data-binary "@$tmpJson"
            if (-not $r) { Start-Sleep -Milliseconds 500 }
        }
        if ($i -eq 1) { $firstResp = $r }
        if ($i -eq 6) { $sixthResp = $r }
        Start-Sleep -Milliseconds 300
    }
    # 锁定后响应（消息含"锁定"）与普通密码错误响应必须不同
    Assert 'Login lockout after 5 fails' $(if ($firstResp -and $sixthResp -and $firstResp -ne $sixthResp) { 'OK' } else { 'FAIL' }) 'OK'

    # 11. 数据权限（ADR-0006）：test 角色仅本人范围，分页只能看到自己
    $dsPage = curl.exe -s --max-time 20 -H "Authorization: Bearer $testToken" `
        'http://localhost:8080/system/user/page?pageNum=1&pageSize=50'
    $dsRecords = $null
    try { $dsRecords = ($dsPage | ConvertFrom-Json).data.records } catch {}
    $dsOk = 'FAIL'
    if ($dsRecords -is [array] -and $dsRecords.Count -eq 1 -and $dsRecords[0].username -eq 'test') {
        $dsOk = 'OK'
    }
    Assert 'Data scope: test sees only self' $dsOk 'OK'

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
