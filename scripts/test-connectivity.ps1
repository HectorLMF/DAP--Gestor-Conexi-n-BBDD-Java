<#
PowerShell test script to call web server endpoints and verify both DB factories
Usage: .\scripts\test-connectivity.ps1 -Host localhost -Port 8000
#>
param(
  [string]$ServerHost = 'localhost',
  [int]$Port = 8000
)

$base = "http://$ServerHost`:$Port"
Write-Host "Checking GET / -> $base/"
try {
  Invoke-WebRequest -Uri "$base/" -UseBasicParsing -TimeoutSec 10 | Out-Null
  Write-Host "GET / OK"
} catch {
  Write-Error "GET / failed: $_"
  exit 1
}

Write-Host "Checking GET /index.html"
try {
  Invoke-WebRequest -Uri "$base/index.html" -UseBasicParsing -TimeoutSec 10 | Out-Null
  Write-Host "GET index.html OK"
} catch {
  Write-Error "GET index failed: $_"
  exit 1
}

function Test-QueryConnection {
  param([string]$Db)
  $payload = @{ db = $Db; sql = 'SELECT 1' } | ConvertTo-Json
  Write-Host "POST /query db=$Db"
  try {
    $resp = Invoke-RestMethod -Uri "$base/query" -Method Post -Body $payload -ContentType 'application/json' -TimeoutSec 10
  $json = $resp | ConvertTo-Json -Depth 5
  Write-Host "Response for $Db`n$($json)"
  } catch {
    Write-Error "POST /query for $Db failed: $_"
    exit 1
  }
}

Test-QueryConnection -Db 'postgres'
Test-QueryConnection -Db 'mysql'
Write-Host "All tests passed"
