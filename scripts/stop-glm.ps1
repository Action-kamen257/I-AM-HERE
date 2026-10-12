# PowerShell Stop Script for GLM llama-server
param(
    [int]$Port = 8080
)

Write-Host "Stopping GLM server on port $Port..." -ForegroundColor Cyan
$connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

if (-not $connections) {
    Write-Host "No active listener found on port $Port." -ForegroundColor Yellow
} else {
    foreach ($conn in $connections) {
        $p = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
        if ($p) {
            Write-Host "Killing process $($p.ProcessName) (PID: $($p.Id))..." -ForegroundColor Red
            Stop-Process -Id $p.Id -Force
        }
    }
    Write-Host "GLM server on port $Port stopped." -ForegroundColor Green
}
