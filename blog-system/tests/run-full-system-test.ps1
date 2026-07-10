$testFile = Join-Path $PSScriptRoot 'full-system-test.ps1'
$testScript = Get-Content -Raw -Encoding UTF8 $testFile
& ([ScriptBlock]::Create($testScript)) @args
exit $LASTEXITCODE
