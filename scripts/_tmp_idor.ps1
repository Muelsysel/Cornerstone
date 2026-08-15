$tmpJson = Join-Path $env:TEMP "cs-idor.json"
[System.IO.File]::WriteAllText($tmpJson, '{"username":"test","password":"admin123"}', (New-Object System.Text.UTF8Encoding($false)))
$testLogin = curl.exe -s --max-time 20 -X POST 'http://localhost:8080/auth/login' -H 'Content-Type: application/json' --data-binary "@$tmpJson"
$testToken = $null
try { $testToken = ($testLogin | ConvertFrom-Json).data.access_token } catch {}
Write-Output ("TOKEN NULL: " + ($null -eq $testToken))
$infoResp = curl.exe -s --max-time 20 -H "Authorization: Bearer $testToken" 'http://localhost:8080/system/user/info?userId=1'
Write-Output ("INFO LEN: " + $infoResp.Length)
Write-Output ("INFO RAW: " + $infoResp)
$infoCode = $null
try { $infoCode = ($infoResp | ConvertFrom-Json).code } catch { Write-Output ("PARSE ERR: " + $_.Exception.Message) }
Write-Output ("INFO CODE: " + $infoCode)
