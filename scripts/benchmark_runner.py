import os
import sys
import time
import json
import threading
import urllib.request
import urllib.error
import subprocess
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

def get_ram_status():
    stat = MEMORYSTATUSEX()
    stat.dwLength = ctypes.sizeof(MEMORYSTATUSEX)
    ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(stat))
    return {
        "used_phys_gb": round((stat.ullTotalPhys - stat.ullAvailPhys) / (1024**3), 2),
        "free_phys_gb": round(stat.ullAvailPhys / (1024**3), 2),
        "used_pagefile_gb": round((stat.ullTotalPageFile - stat.ullAvailPageFile) / (1024**3), 2),
        "free_pagefile_gb": round(stat.ullAvailPageFile / (1024**3), 2),
    }

def get_gpu_status():
    try:
        out = subprocess.check_output(
            ["nvidia-smi", "--query-gpu=memory.used,memory.free,utilization.gpu,temperature.gpu,power.draw", "--format=csv,noheader,nounits"],
            text=True
        ).strip()
        parts = [p.strip() for p in out.split(",")]
        return {
            "vram_used_mib": float(parts[0]),
            "vram_free_mib": float(parts[1]),
            "gpu_util_percent": float(parts[2]),
            "temp_c": float(parts[3]),
            "power_w": float(parts[4])
        }
    except Exception as e:
        return {"error": str(e), "vram_used_mib": 0.0, "gpu_util_percent": 0.0, "temp_c": 0.0, "power_w": 0.0}

class SystemMonitor(threading.Thread):
    def __init__(self, interval=0.25):
        super().__init__()
        self.interval = interval
        self.running = True
        self.snapshots = []

    def run(self):
        while self.running:
            ram = get_ram_status()
            gpu = get_gpu_status()
            self.snapshots.append({
                "time": time.time(),
                "ram": ram,
                "gpu": gpu
            })
            time.sleep(self.interval)

    def stop(self):
        self.running = False
        self.join()

    def get_summary(self):
        if not self.snapshots:
            return {}
        vram_vals = [s["gpu"].get("vram_used_mib", 0) for s in self.snapshots]
        gpu_util_vals = [s["gpu"].get("gpu_util_percent", 0) for s in self.snapshots]
        ram_vals = [s["ram"].get("used_phys_gb", 0) for s in self.snapshots]
        page_vals = [s["ram"].get("used_pagefile_gb", 0) for s in self.snapshots]
        temp_vals = [s["gpu"].get("temp_c", 0) for s in self.snapshots]
        power_vals = [s["gpu"].get("power_w", 0) for s in self.snapshots]
        return {
            "peak_vram_mib": max(vram_vals) if vram_vals else 0,
            "avg_vram_mib": round(sum(vram_vals)/len(vram_vals), 1) if vram_vals else 0,
            "peak_gpu_util": max(gpu_util_vals) if gpu_util_vals else 0,
            "avg_gpu_util": round(sum(gpu_util_vals)/len(gpu_util_vals), 1) if gpu_util_vals else 0,
            "peak_ram_gb": max(ram_vals) if ram_vals else 0,
            "avg_ram_gb": round(sum(ram_vals)/len(ram_vals), 2) if ram_vals else 0,
            "peak_pagefile_gb": max(page_vals) if page_vals else 0,
            "pagefile_delta_gb": round(max(page_vals) - min(page_vals), 2) if page_vals else 0,
            "peak_temp_c": max(temp_vals) if temp_vals else 0,
            "avg_power_w": round(sum(power_vals)/len(power_vals), 1) if power_vals else 0,
            "samples_count": len(self.snapshots)
        }

def run_benchmark(engine, endpoint_url, prompt_label, prompt_text, model_name, max_tokens=768, extra_options=None):
    monitor = SystemMonitor(interval=0.25)
    monitor.start()

    start_time = time.time()
    first_token_time = None
    full_response = []
    token_timestamps = []
    final_stats = {}

    is_ollama_native = "/api/generate" in endpoint_url

    if is_ollama_native:
        payload = {
            "model": model_name,
            "prompt": prompt_text,
            "stream": True,
            "options": {
                "num_predict": max_tokens,
                "temperature": 0.2,
                "stop": ["<|endoftext|>", "<|user|>", "<|observation|>", "<|im_end|>"]
            }
        }
        if extra_options:
            payload["options"].update(extra_options)
    else:
        # OpenAI compatible endpoint (/v1/chat/completions)
        payload = {
            "model": model_name,
            "messages": [{"role": "user", "content": prompt_text}],
            "max_tokens": max_tokens,
            "temperature": 0.2,
            "stream": True,
            "stop": ["<|endoftext|>", "<|user|>", "<|observation|>", "<|im_end|>"]
        }

    req_data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(endpoint_url, data=req_data, headers={"Content-Type": "application/json"})

    try:
        with urllib.request.urlopen(req, timeout=300) as response:
            for line in response:
                if not line:
                    continue
                line_str = line.decode("utf-8", errors="replace").strip()
                if not line_str:
                    continue

                if is_ollama_native:
                    try:
                        data = json.loads(line_str)
                    except Exception:
                        continue
                    piece = data.get("response", "")
                    if piece:
                        if first_token_time is None:
                            first_token_time = time.time()
                        full_response.append(piece)
                        token_timestamps.append(time.time())
                    if data.get("done", False):
                        final_stats = {
                            "prompt_eval_count": data.get("prompt_eval_count", 0),
                            "prompt_eval_duration_ms": round(data.get("prompt_eval_duration", 0) / 1e6, 2),
                            "eval_count": data.get("eval_count", len(full_response)),
                            "eval_duration_ms": round(data.get("eval_duration", 0) / 1e6, 2),
                        }
                        break
                else:
                    # SSE stream "data: {...}"
                    if line_str.startswith("data: "):
                        data_str = line_str[6:].strip()
                        if data_str == "[DONE]":
                            break
                        try:
                            chunk = json.loads(data_str)
                        except Exception:
                            continue
                        delta = chunk.get("choices", [{}])[0].get("delta", {})
                        piece = delta.get("content", "")
                        if piece:
                            if first_token_time is None:
                                first_token_time = time.time()
                            full_response.append(piece)
                            token_timestamps.append(time.time())
                        usage = chunk.get("usage")
                        if usage:
                            final_stats["usage"] = usage
    except Exception as e:
        monitor.stop()
        return {"error": str(e)}

    end_time = time.time()
    monitor.stop()

    summary_metrics = monitor.get_summary()

    ttft_sec = round(first_token_time - start_time, 3) if first_token_time else 0.0
    total_elapsed = round(end_time - start_time, 3)
    gen_elapsed = round(end_time - first_token_time, 3) if first_token_time else total_elapsed

    eval_count = final_stats.get("eval_count", len(token_timestamps))
    if eval_count == 0:
        eval_count = len(token_timestamps)
    
    gen_duration_sec = (final_stats.get("eval_duration_ms", 0) / 1000.0) if final_stats.get("eval_duration_ms") else gen_elapsed
    gen_tok_per_sec = round(eval_count / gen_duration_sec, 2) if gen_duration_sec > 0 else 0

    prompt_count = final_stats.get("prompt_eval_count", 0)
    prompt_duration_sec = (final_stats.get("prompt_eval_duration_ms", 0) / 1000.0) if final_stats.get("prompt_eval_duration_ms") else 0
    prompt_tok_per_sec = round(prompt_count / prompt_duration_sec, 2) if prompt_duration_sec > 0 else 0

    return {
        "engine": engine,
        "endpoint": endpoint_url,
        "model": model_name,
        "prompt_label": prompt_label,
        "ttft_sec": ttft_sec,
        "total_elapsed_sec": total_elapsed,
        "gen_elapsed_sec": round(gen_duration_sec, 3),
        "prompt_tokens": prompt_count,
        "prompt_tok_per_sec": prompt_tok_per_sec,
        "eval_tokens": eval_count,
        "eval_tok_per_sec": gen_tok_per_sec,
        "system_metrics": summary_metrics,
        "final_stats_raw": final_stats,
        "output_snippet": ("".join(full_response))[:300] + "..."
    }

PROMPT_A = """Write an asynchronous Python LRU Cache with TTL (Time To Live) support. It must be thread-safe/asyncio-safe, support get(), put(), evict_expired(), clear(), and have O(1) average lookup. Include comprehensive unit tests using pytest."""

PROMPT_B = """You are refactoring a multi-module Python backend. Below is the existing project structure and code for 3 files:
`models.py`:
```python
from dataclasses import dataclass
from typing import Optional
@dataclass
class Task:
    id: str
    title: str
    status: str
    priority: int
    retry_count: int = 0
```
`queue.py`:
```python
import queue
class TaskQueue:
    def __init__(self):
        self.q = queue.Queue()
    def push(self, task):
        self.q.put(task)
    def pop(self):
        return self.q.get()
```
`worker.py`:
```python
import time
class Worker:
    def __init__(self, q):
        self.q = q
    def run_once(self):
        t = self.q.pop()
        print(f"Processing {t.id}")
        time.sleep(0.1)
```
Task: Refactor this into a production-grade asynchronous worker pipeline using `asyncio.PriorityQueue`, structured concurrency (`asyncio.TaskGroup`), dead-letter queue (DLQ) for failed tasks after 3 retries, exponential backoff, health check status endpoint, and graceful cancellation on SIGINT/SIGTERM. Provide the complete refactored code for all files and explain your architectural design decisions."""

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--engine", default="Ollama")
    parser.add_argument("--url", default="http://127.0.0.1:11434/api/generate")
    parser.add_argument("--model", default="glm-4.7-flash:q4_K_M")
    parser.add_argument("--task", choices=["A", "B"], default="A")
    parser.add_argument("--max_tokens", type=int, default=512)
    parser.add_argument("--outfile", default="baseline_ollama_task_A.json")
    args = parser.parse_args()

    prompt = PROMPT_A if args.task == "A" else PROMPT_B
    print(f"Running Benchmark: Engine={args.engine}, Task={args.task}, MaxTokens={args.max_tokens}...")
    res = run_benchmark(args.engine, args.url, f"Task_{args.task}", prompt, args.model, max_tokens=args.max_tokens)
    print("Result summary:")
    print(json.dumps(res, indent=2))

    os.makedirs(r"D:\AI\Antigravity-Local\benchmarks", exist_ok=True)
    out_path = os.path.join(r"D:\AI\Antigravity-Local\benchmarks", args.outfile)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(res, f, indent=2)
    print(f"Saved to {out_path}")
