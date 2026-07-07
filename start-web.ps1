$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$java = 'C:\Program Files\Java\jdk1.8.0_201\bin\java.exe'
$backendJar = Join-Path $root 'blog-system\backend\target\blog-system-backend-1.0.0.jar'
$frontendDir = Join-Path $root 'demo-site'
$logDir = Join-Path $root 'tmp\run-logs'
$backendOut = Join-Path $logDir 'backend.out.log'
$backendErr = Join-Path $logDir 'backend.err.log'
$frontendOut = Join-Path $logDir 'frontend.out.log'
$frontendErr = Join-Path $logDir 'frontend.err.log'

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (!(Test-Path $java)) { throw "JDK not found: $java" }
if (!(Test-Path $backendJar)) { throw "Backend jar not found: $backendJar" }

if (-not $env:MYSQL_USER) { $env:MYSQL_USER = 'root' }
if (-not $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD = 'root' }

function Test-Port($port) {
  $client = New-Object Net.Sockets.TcpClient
  try {
    $iar = $client.BeginConnect('127.0.0.1', $port, $null, $null)
    if ($iar.AsyncWaitHandle.WaitOne(300, $false)) {
      $client.EndConnect($iar)
      return $true
    }
    return $false
  } catch {
    return $false
  } finally {
    $client.Close()
  }
}

function Stop-Port($port) {
  $lines = netstat -ano | Select-String ":$port\s"
  foreach ($line in $lines) {
    $parts = ($line.ToString() -split '\s+') | Where-Object { $_ }
    $pidText = $parts[-1]
    if ($pidText -match '^\d+$') {
      try { Stop-Process -Id ([int]$pidText) -Force -ErrorAction SilentlyContinue } catch {}
    }
  }
}

function Start-Backend() {
  if (Test-Port 8080) { return }
  Start-Process -FilePath $java `
    -ArgumentList @('-jar', $backendJar) `
    -WorkingDirectory (Join-Path $root 'blog-system\backend') `
    -WindowStyle Hidden `
    -RedirectStandardOutput $backendOut `
    -RedirectStandardError $backendErr
  Start-Sleep -Seconds 5
}

function Start-Frontend() {
  if (Test-Port 4000) { return }
  $npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
  if (-not $npm) { $npm = (Get-Command npm -ErrorAction Stop).Source }
  Start-Process -FilePath $npm `
    -ArgumentList @('run', 'preview') `
    -WorkingDirectory $frontendDir `
    -WindowStyle Hidden `
    -RedirectStandardOutput $frontendOut `
    -RedirectStandardError $frontendErr
  Start-Sleep -Seconds 8
}

function Test-BackendApi() {
  try {
    $resp = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8080/api/articles' -TimeoutSec 5
    return $resp.StatusCode -eq 200
  } catch {
    return $false
  }
}

Start-Backend

if (-not (Test-BackendApi)) {
  Stop-Port 8080
  Start-Sleep -Seconds 2
  Start-Backend
}

Start-Frontend

if (Test-BackendApi) {
  Write-Host 'Backend API OK.'
} else {
  Write-Host 'Backend API is not ready.'
  Write-Host 'Check MySQL is running, password is root, and database mydataset exists.'
  Write-Host "Backend stdout: $backendOut"
  Write-Host "Backend stderr: $backendErr"
}

Start-Process 'http://127.0.0.1:4000/login.html'
Write-Host 'Started: http://127.0.0.1:4000/login.html'
Write-Host 'Backend runs hidden on http://127.0.0.1:8080/api and syncs data to MySQL mydataset.'