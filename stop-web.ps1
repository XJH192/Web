$ports = 4000, 8080
foreach ($port in $ports) {
  $lines = netstat -ano | Select-String ":$port\s"
  foreach ($line in $lines) {
    $parts = ($line.ToString() -split '\s+') | Where-Object { $_ }
    $processIdText = $parts[-1]
    if ($processIdText -match '^\d+$') {
      try {
        Stop-Process -Id ([int]$processIdText) -Force -ErrorAction Stop
        Write-Host "Stopped port $port process PID $processIdText"
      } catch {}
    }
  }
}