$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaHome = [System.Environment]::GetEnvironmentVariable('JAVA_HOME')
if (-not $javaHome) { $javaHome = 'D:\Java' }
$java = "$javaHome\bin\java.exe"
$backendJar = Join-Path $root 'blog-system\backend\target\blog-system-backend-1.0.0.jar'
$mvn = 'mvn'
$frontendDir = Join-Path $root 'demo-site'
$logDir = Join-Path $root 'tmp\run-logs'
$backendOut = Join-Path $logDir 'backend.out.log'
$backendErr = Join-Path $logDir 'backend.err.log'
$frontendOut = Join-Path $logDir 'frontend.out.log'
$frontendErr = Join-Path $logDir 'frontend.err.log'
$script:BackendRebuilt = $false

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Load-LocalEnv() {
  $envFile = Join-Path $root '.env.local'
  if (!(Test-Path $envFile)) { return }
  foreach ($rawLine in Get-Content -LiteralPath $envFile) {
    $line = $rawLine.Trim()
    if ($line -and !$line.StartsWith('#') -and $line -match '=') {
      $name, $value = $line -split '=', 2
      $name = $name.Trim()
      $value = $value.Trim().Trim('"').Trim("'")
      if ($name) { [Environment]::SetEnvironmentVariable($name, $value, 'Process') }
    }
  }
}
Load-LocalEnv

function Test-LocalProxyPort($value) {
  if ($value -notmatch '^(http|https)://(127\.0\.0\.1|localhost):(\d+)') { return $true }
  $port = [int]$matches[3]
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

function Clear-BrokenProxyEnv() {
  foreach ($name in @('HTTP_PROXY','HTTPS_PROXY','ALL_PROXY','http_proxy','https_proxy','all_proxy')) {
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    if ($value -and -not (Test-LocalProxyPort $value)) {
      Remove-Item -LiteralPath ("Env:$name") -ErrorAction SilentlyContinue
      Write-Host "Ignored broken local proxy $name=$value"
    }
  }
  $env:NO_PROXY = 'localhost,127.0.0.1,::1,api.deepseek.com'
  $env:no_proxy = $env:NO_PROXY
}
Clear-BrokenProxyEnv
if (!(Test-Path $java)) { throw "JDK not found: $java" }
if (-not (Get-Command $mvn -ErrorAction SilentlyContinue)) { throw "Maven command not found: $mvn" }

function Test-BootJar($path) {
  if (!(Test-Path $path)) { return $false }
  try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
    $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
    try {
      $entry = $zip.GetEntry('META-INF/MANIFEST.MF')
      if ($null -eq $entry) { return $false }
      $reader = New-Object System.IO.StreamReader($entry.Open())
      try { $manifest = $reader.ReadToEnd() } finally { $reader.Close() }
      return $manifest -match 'Main-Class:\s*org\.springframework\.boot\.loader\.JarLauncher'
    } finally {
      $zip.Dispose()
    }
  } catch {
    return $false
  }
}
function Ensure-BackendJar() {
  $backendDir = Join-Path $root 'blog-system\backend'
  $needsBuild = !(Test-Path $backendJar)
  if (-not $needsBuild -and -not (Test-BootJar $backendJar)) { $needsBuild = $true }
  if (-not $needsBuild) {
    $jarTime = (Get-Item $backendJar).LastWriteTime
    $latestSource = Get-ChildItem -LiteralPath $backendDir -Recurse -File |
      Where-Object { $_.FullName -match '\\src\\|pom\.xml$' } |
      Sort-Object LastWriteTime -Descending |
      Select-Object -First 1
    if ($latestSource -and $latestSource.LastWriteTime -gt $jarTime) { $needsBuild = $true }
  }
  if (-not $needsBuild) { return }
  # Stop existing backend before rebuilding, otherwise Windows keeps the old jar locked.
  if (Test-Port 8080) {
    Stop-Port 8080
    Start-Sleep -Seconds 2
  }
  Write-Host 'Backend jar missing, stale, or not executable. Building backend automatically...'
  & $mvn -q -f (Join-Path $backendDir 'pom.xml') -DskipTests package
  if ($LASTEXITCODE -ne 0) { throw 'Backend package failed. Check JDK/Maven and dependency cache.' }
  if (!(Test-Path $backendJar)) { throw "Backend jar still not found after package: $backendJar" }
  if (-not (Test-BootJar $backendJar)) { throw "Backend jar is not executable after package: $backendJar" }
  $script:BackendRebuilt = $true
}

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
    -ArgumentList @('-Djava.net.useSystemProxies=false', '-Dhttps.protocols=TLSv1.2', '-jar', $backendJar) `
    -WorkingDirectory (Join-Path $root 'blog-system\backend') `
    -WindowStyle Hidden `
    -RedirectStandardOutput $backendOut `
    -RedirectStandardError $backendErr
  Start-Sleep -Seconds 5
}

function Restart-BackendIfEnvChanged() {
  $envFile = Join-Path $root '.env.local'
  if (!(Test-Path $envFile) -or !(Test-Port 8080)) { return }
  if (!(Test-Path $backendOut)) { return }
  $envTime = (Get-Item $envFile).LastWriteTime
  $backendLogTime = (Get-Item $backendOut).LastWriteTime
  if ($envTime -gt $backendLogTime) {
    Write-Host '.env.local changed after backend started. Restarting backend to reload environment variables...'
    Stop-Port 8080
    Start-Sleep -Seconds 2
  }
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

Ensure-BackendJar
if ($script:BackendRebuilt -and (Test-Port 8080)) {
  Stop-Port 8080
  Start-Sleep -Seconds 2
}
Restart-BackendIfEnvChanged
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
