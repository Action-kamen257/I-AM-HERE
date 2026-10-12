import subprocess
import json
import ctypes
from ctypes import wintypes

class MEMORYSTATUSEX(ctypes.Structure):
    _fields_ = [
        ("dwLength", wintypes.DWORD),
        ("dwMemoryLoad", wintypes.DWORD),
        ("ullTotalPhys", ctypes.c_uint64),
        ("ullAvailPhys", ctypes.c_uint64),
        ("ullTotalPageFile", ctypes.c_uint64),
        ("ullAvailPageFile", ctypes.c_uint64),
        ("ullTotalVirtual", ctypes.c_uint64),
        ("ullAvailVirtual", ctypes.c_uint64),
        ("ullAvailExtendedVirtual", ctypes.c_uint64),
    ]

def get_ram():
    stat = MEMORYSTATUSEX()
    stat.dwLength = ctypes.sizeof(MEMORYSTATUSEX)
    ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(stat))
    return {
        "memory_load_percent": stat.dwMemoryLoad,
        "total_phys_gb": round(stat.ullTotalPhys / (1024**3), 2),
        "avail_phys_gb": round(stat.ullAvailPhys / (1024**3), 2),
        "total_pagefile_gb": round(stat.ullTotalPageFile / (1024**3), 2),
        "avail_pagefile_gb": round(stat.ullAvailPageFile / (1024**3), 2),
    }

def get_cpu():
    import os
    logical_cores = os.cpu_count()
    import winreg
    try:
        key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, r"HARDWARE\DESCRIPTION\System\CentralProcessor\0")
        name, _ = winreg.QueryValueEx(key, "ProcessorNameString")
        identifier, _ = winreg.QueryValueEx(key, "Identifier")
        mhz, _ = winreg.QueryValueEx(key, "~MHz")
        
        # Get physical cores using GetLogicalProcessorInformation or PowerShell without $_
        out = subprocess.check_output(["powershell", "-NoProfile", "-Command", "(Get-CimInstance Win32_Processor).NumberOfCores"], text=True).strip()
        physical_cores = int(out) if out.isdigit() else 8
        return {
            "name": name.strip(),
            "identifier": identifier.strip(),
            "mhz": mhz,
            "physical_cores": physical_cores,
            "logical_processors": logical_cores
        }
    except Exception as e:
        return {"error": str(e), "logical_processors": logical_cores}

def get_gpu():
    try:
        out = subprocess.check_output(
            ["nvidia-smi", "--query-gpu=name,memory.total,memory.free,memory.used,driver_version,compute_cap,temperature.gpu,power.draw", "--format=csv,noheader,nounits"],
            text=True
        ).strip()
        parts = [p.strip() for p in out.split(",")]
        return {
            "name": parts[0],
            "total_vram_mib": float(parts[1]),
            "free_vram_mib": float(parts[2]),
            "used_vram_mib": float(parts[3]),
            "driver_version": parts[4],
            "compute_cap": parts[5],
            "temp_c": float(parts[6]),
            "power_w": float(parts[7])
        }
    except Exception as e:
        return {"error": str(e)}

def get_disks():
    disks = {}
    for drive in ["C", "D"]:
        free_bytes = ctypes.c_ulonglong(0)
        total_bytes = ctypes.c_ulonglong(0)
        total_free_bytes = ctypes.c_ulonglong(0)
        ctypes.windll.kernel32.GetDiskFreeSpaceExW(
            ctypes.c_wchar_p(f"{drive}:\\"),
            ctypes.byref(free_bytes),
            ctypes.byref(total_bytes),
            ctypes.byref(total_free_bytes)
        )
        disks[drive] = {
            "total_gb": round(total_bytes.value / (1024**3), 2),
            "free_gb": round(free_bytes.value / (1024**3), 2),
            "used_gb": round((total_bytes.value - free_bytes.value) / (1024**3), 2)
        }
    try:
        out = subprocess.check_output(
            ["powershell", "-NoProfile", "-Command", "Get-PhysicalDisk | Select-Object -Property FriendlyName, MediaType, BusType | ConvertTo-Json"],
            text=True
        ).strip()
        disks["physical_drives"] = json.loads(out)
    except Exception as e:
        disks["physical_drives"] = str(e)
    return disks

if __name__ == "__main__":
    data = {
        "gpu": get_gpu(),
        "cpu": get_cpu(),
        "ram": get_ram(),
        "disks": get_disks()
    }
    print(json.dumps(data, indent=2))
