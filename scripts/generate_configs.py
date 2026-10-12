import os
import json

CONFIG_DIR = r"D:\AI\Antigravity-Local\config"
SCRIPTS_DIR = r"D:\AI\Antigravity-Local\scripts"

os.makedirs(CONFIG_DIR, exist_ok=True)
os.makedirs(SCRIPTS_DIR, exist_ok=True)

profiles = {
    "glm47-fast": {
        "profile": "fast",
        "name": "GLM-4.7-Flash Fast (8K)",
        "description": "Optimized for maximum token generation speed and low latency in everyday coding and quick chat.",
        "server_exe": r"D:\AI\llama.cpp\bin-ollama\llama-server.exe",
        "model_path": r"D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf",
        "host": "127.0.0.1",
        "port": 8080,
        "ctx": 8192,
        "threads": 8,
        "batch": 512,
        "ubatch": 512,
        "parallel": 1,
        "fit": "on",
        "flash_attn": "on",
        "mmap": True,
        "jinja": True
    },
    "glm47-default": {
        "profile": "default",
        "name": "GLM-4.7-Flash Balanced (16K)",
        "description": "Recommended all-rounder for general Antigravity coding tasks and multi-turn workflows.",
        "server_exe": r"D:\AI\llama.cpp\bin-ollama\llama-server.exe",
        "model_path": r"D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf",
        "host": "127.0.0.1",
        "port": 8080,
        "ctx": 16384,
        "threads": 8,
        "batch": 512,
        "ubatch": 512,
        "parallel": 1,
        "fit": "on",
        "flash_attn": "on",
        "mmap": True,
        "jinja": True
    },
    "glm47-32k": {
        "profile": "32k",
        "name": "GLM-4.7-Flash Deep Context (32K)",
        "description": "Large 32,768 context window for whole-module refactoring and repository inspection.",
        "server_exe": r"D:\AI\llama.cpp\bin-ollama\llama-server.exe",
        "model_path": r"D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf",
        "host": "127.0.0.1",
        "port": 8080,
        "ctx": 32768,
        "threads": 8,
        "batch": 1024,
        "ubatch": 512,
        "parallel": 1,
        "fit": "on",
        "flash_attn": "on",
        "mmap": True,
        "jinja": True
    }
}

for name, conf in profiles.items():
    path = os.path.join(CONFIG_DIR, f"{name}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(conf, f, indent=2)
    print(f"Created config: {path}")

def make_start_script(profile_key, conf):
    return f"""# PowerShell Start Script for {conf['name']}
param(
    [switch]$Background
)

$HostIP = "{conf['host']}"
$Port = {conf['port']}
$ServerExe = "{conf['server_exe']}"
$ModelPath = "{conf['model_path']}"
$Ctx = {conf['ctx']}
$Threads = {conf['threads']}
$Batch = {conf['batch']}
$Ubatch = {conf['ubatch']}
$LogOut = "D:\\AI\\Antigravity-Local\\logs\\llama-server-$Port.log"
$LogErr = "D:\\AI\\Antigravity-Local\\logs\\llama-server-$Port.err.log"

# Check if port is already in use
$listening = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($listening) {{
    Write-Host "[!] Port $Port is already in use by PID $($listening[0].OwningProcess)." -ForegroundColor Yellow
    Write-Host "    Run .\\stop-glm.ps1 first if you want to restart."
    exit 1
}}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Starting {conf['name']} on http://$HostIP`:$Port" -ForegroundColor Green
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

if ($Background) {{
    Write-Host "Starting server as background process (Logs: $LogOut)..." -ForegroundColor Yellow
    Start-Process -FilePath $ServerExe -ArgumentList $argsList -RedirectStandardOutput $LogOut -RedirectStandardError $LogErr -NoNewWindow
    Start-Sleep -Seconds 3
    Write-Host "Server launched. Verify with .\\status-glm.ps1" -ForegroundColor Green
}} else {{
    & $ServerExe @argsList
}}
"""

for key, conf in profiles.items():
    script_content = make_start_script(key, conf)
    script_path = os.path.join(SCRIPTS_DIR, f"start-{key.replace('glm47-', 'glm-')}.ps1")
    with open(script_path, "w", encoding="utf-8") as f:
        f.write(script_content)
    print(f"Created start script: {script_path}")

# Stop script
stop_script = """# PowerShell Stop Script for GLM llama-server
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
"""

stop_path = os.path.join(SCRIPTS_DIR, "stop-glm.ps1")
with open(stop_path, "w", encoding="utf-8") as f:
    f.write(stop_script)
print(f"Created stop script: {stop_path}")

# Status script
status_script = """# PowerShell Status Script for GLM llama-server and Ollama
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
"""

status_path = os.path.join(SCRIPTS_DIR, "status-glm.ps1")
with open(status_path, "w", encoding="utf-8") as f:
    f.write(status_script)
print(f"Created status script: {status_path}")

print("\nAll configuration profiles and management scripts generated successfully!")
