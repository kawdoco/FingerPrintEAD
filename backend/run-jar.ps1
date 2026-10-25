# Runs the packaged jar (built by ..\package.ps1). Use this on the lab PC / server.
#   .\run-jar.ps1            -> uses Supabase settings from backend\.env
#   .\run-jar.ps1 -Local     -> in-memory demo database (no Supabase)
param([switch]$Local)
Set-Location -Path $PSScriptRoot
$jar = "target\attendance-system.jar"
if (-not (Test-Path $jar)) { Write-Host "$jar not found. Run ..\package.ps1 first." -ForegroundColor Red; exit 1 }

if ($Local) {
    Write-Host "Starting jar with the LOCAL in-memory database (demo mode)..." -ForegroundColor Cyan
    java -jar $jar --spring.profiles.active=local
    exit $LASTEXITCODE
}

if (-not (Test-Path ".env")) {
    Write-Host "backend\.env not found. Copy .env.example to .env and fill in your Supabase details." -ForegroundColor Red
    exit 1
}
Get-Content ".env" | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $i = $line.IndexOf("=")
        Set-Item -Path ("Env:" + $line.Substring(0, $i).Trim()) -Value $line.Substring($i + 1).Trim()
    }
}
Write-Host "Starting with Supabase: $env:SUPABASE_DB_URL" -ForegroundColor Cyan
java -jar $jar
