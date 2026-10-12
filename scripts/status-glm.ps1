# PowerShell Status Script for GLM llama-server and Ollama
param(
    [int]$Port = 8080
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " GLM-4.7-Flash Service & System Status" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. llama-server check
$llamaConn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($llamaConn) {
    $proc = Get-Process -Id $llamaConn[0].OwningProcess -ErrorAction SilentlyContinue
    Write-Host "[+] Standalone llama-server: RUNNING on port $Port (PID: $($proc.Id), RAM: $([math]::Round($proc.WorkingSet64/1MB, 1)) MB)" -ForegroundColor Green
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/health" -TimeoutSec 2 -ErrorAction Stop
        Write-Host "    API Status: OK (Health: $($health.status))" -ForegroundColor Green
    } catch {
        Write-Host "    API Status: Responding or Loading..." -ForegroundColor Yellow
    }
} else {
    Write-Host "[-] Standalone llama-server: STOPPED (Port $Port free)" -ForegroundColor DarkGray
}

# 2. Ollama check
$ollamaConn = Get-NetTCPConnection -LocalPort 11434 -State Listen -ErrorAction SilentlyContinue
if ($ollamaConn) {
    Write-Host "[+] Ollama Service: RUNNING on port 11434 (PID: $($ollamaConn[0].OwningProcess))" -ForegroundColor Cyan
} else {
    Write-Host "[-] Ollama Service: STOPPED" -ForegroundColor DarkGray
}

# 3. GPU VRAM check
Write-Host "`n--- GPU VRAM Status ---" -ForegroundColor Cyan
& "nvidia-smi" --query-gpu=name,memory.total,memory.used,memory.free,temperature.gpu,power.draw --format=csv,noheader

# 4. System RAM check
$os = Get-CimInstance Win32_OperatingSystem
$totalRAM = [math]::Round($os.TotalVisibleMemorySize / 1MB, 2)
$freeRAM = [math]::Round($os.FreePhysicalMemory / 1MB, 2)
$usedRAM = [math]::Round($totalRAM - $freeRAM, 2)
Write-Host "`n--- System RAM Status ---" -ForegroundColor Cyan
Write-Host "Used: $usedRAM GB / $totalRAM GB (Free: $freeRAM GB)" -ForegroundColor White

Write-Host "==========================================================" -ForegroundColor Cyan
