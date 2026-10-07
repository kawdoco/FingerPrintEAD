# Starts the Spring Boot backend on Windows (PowerShell).
#   .\run-backend.ps1            -> uses Supabase settings from backend\.env
#   .\run-backend.ps1 -Local     -> in-memory database, no Supabase needed (demo data, resets on stop)
param([switch]$Local)

Set-Location -Path $PSScriptRoot

if ($Local) {
    Write-Host "Starting with the LOCAL in-memory database (demo mode)..." -ForegroundColor Cyan
    mvn spring-boot:run "-Dspring-boot.run.profiles=local"
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
        $name = $line.Substring(0, $i).Trim()
        $value = $line.Substring($i + 1).Trim()
        Set-Item -Path "Env:$name" -Value $value
    }
}

Write-Host "Starting with Supabase: $env:SUPABASE_DB_URL" -ForegroundColor Cyan
mvn spring-boot:run
