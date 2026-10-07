# One-command launcher (Windows PowerShell).
#   .\start.ps1             -> demo mode (in-memory DB, 8 demo students, device key: local-demo-device-key)
#   .\start.ps1 -Supabase   -> real database (needs backend\.env)
param([switch]$Supabase)
$root = $PSScriptRoot
foreach ($tool in "java","mvn","node","npm") {
  if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { Write-Host "Missing '$tool'. Install JDK 17+, Maven and Node 18+ first." -ForegroundColor Red; exit 1 }
}
$ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notmatch '^(127|169)\.' -and $_.PrefixOrigin -ne 'WellKnown' } | Select-Object -First 1).IPAddress
$flag = if ($Supabase) { "" } else { "-Local" }
Start-Process powershell -ArgumentList "-NoExit","-Command","Set-Location '$root\backend'; .\run-backend.ps1 $flag"
if (-not (Test-Path "$root\frontend\node_modules")) { Push-Location "$root\frontend"; npm install; Pop-Location }
Start-Process powershell -ArgumentList "-NoExit","-Command","Set-Location '$root\frontend'; npm run dev -- --host"
Write-Host ""
Write-Host "Live display : http://localhost:5173/"            -ForegroundColor Cyan
Write-Host "Admin        : http://localhost:5173/admin/login  (admin / ChangeMe123!)" -ForegroundColor Cyan
Write-Host "ESP32 URL    : http://${ip}:8080   <- use this as BACKEND_BASE_URL in the firmware" -ForegroundColor Yellow
Write-Host "Allow inbound TCP 8080 in Windows Firewall so the ESP32 can reach the backend."
Start-Sleep 12; Start-Process "http://localhost:5173/"
