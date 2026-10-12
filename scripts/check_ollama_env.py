import subprocess
import json

ps_cmd = "Get-CimInstance Win32_Process -Filter \"Name = 'ollama.exe' or Name = 'llama-server.exe'\" | Select-Object ProcessId, Name, CommandLine | ConvertTo-Json"
out = subprocess.check_output(["powershell", "-NoProfile", "-Command", ps_cmd], text=True)
print(out)
