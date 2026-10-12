# PowerShell Start Script for GLM-4.7-Flash Balanced (16K)
param(
    [switch]$Background
)

$HostIP = "127.0.0.1"
$Port = 8080
$ServerExe = "D:\AI\llama.cpp\bin-ollama\llama-server.exe"
$ModelPath = "D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf"
$Ctx = 16384
$Threads = 8
$Batch = 512
$Ubatch = 512
$LogOut = "D:\AI\Antigravity-Local\logs\llama-server-$Port.log"
$LogErr = "D:\AI\Antigravity-Local\logs\llama-server-$Port.err.log"

# Check if port is already in use
$listening = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($listening) {
    Write-Host "[!] Port $Port is already in use by PID $($listening[0].OwningProcess)." -ForegroundColor Yellow
    Write-Host "    Run .\stop-glm.ps1 first if you want to restart."
    exit 1
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Starting GLM-4.7-Flash Balanced (16K) on http://$HostIP`:$Port" -ForegroundColor Green
Write-Host " Context: $Ctx | Threads: $Threads | Flash Attention: ON" -ForegroundColor Cyan
Write-Host " Model: $ModelPath" -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Cyan

$argsList = @(
    "--model", $ModelPath,
    "--host", $HostIP,
    "--port", "$Port",
    "-c", "$Ctx",
    "-t", "$Threads",
    "-b", "$Batch",
    "-ub", "$Ubatch",
    "-np", "1",
    "-fit", "on",
    "--flash-attn", "on",
    "--no-warmup"
)

if ($Background) {
    Write-Host "Starting server as background process (Logs: $LogOut)..." -ForegroundColor Yellow
    Start-Process -FilePath $ServerExe -ArgumentList $argsList -RedirectStandardOutput $LogOut -RedirectStandardError $LogErr -NoNewWindow
    Start-Sleep -Seconds 3
    Write-Host "Server launched. Verify with .\status-glm.ps1" -ForegroundColor Green
} else {
    & $ServerExe @argsList
}
