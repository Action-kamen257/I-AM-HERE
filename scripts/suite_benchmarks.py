import os
import sys
import time
import json
import subprocess
import urllib.request
import urllib.error
from benchmark_runner import SystemMonitor

SERVER_EXE = r"D:\AI\llama.cpp\bin-ollama\llama-server.exe"
MODEL_PATH = r"D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf"
LOG_DIR = r"D:\AI\Antigravity-Local\logs"
BENCH_DIR = r"D:\AI\Antigravity-Local\benchmarks"
PORT = 8080

PROMPT_A = """Write an asynchronous Python LRU Cache with TTL (Time To Live) support. It must be thread-safe/asyncio-safe, support get(), put(), evict_expired(), clear(), and have O(1) average lookup. Include comprehensive unit tests using pytest."""

def run_test_configuration(config_name, ngl=None, flash_attn=True, ctx=4096, threads=8, batch=512, ubatch=512, mmap=True, max_gen_tokens=256):
    os.makedirs(LOG_DIR, exist_ok=True)
    os.makedirs(BENCH_DIR, exist_ok=True)
    
    server_log_path = os.path.join(LOG_DIR, f"{config_name}_server.log")
    server_log_file = open(server_log_path, "w", encoding="utf-8")

    cmd = [
        SERVER_EXE,
        "--model", MODEL_PATH,
        "--port", str(PORT),
        "--host", "127.0.0.1",
        "-c", str(ctx),
        "-b", str(batch),
        "-ub", str(ubatch),
        "-t", str(threads),
        "-np", "1",
        "--no-warmup"
    ]
    if ngl is not None:
        cmd.extend(["-ngl", str(ngl)])
    else:
        # Auto-fit to device memory
        cmd.extend(["-fit", "on"])

    if flash_attn:
        cmd.extend(["--flash-attn", "on"])
    else:
        cmd.extend(["--flash-attn", "off"])

    if not mmap:
        cmd.append("--no-mmap")

    print(f"\n=======================================================")
    print(f"STARTING CONFIG: {config_name}")
    print(f"CMD: {' '.join(cmd)}")
    print(f"=======================================================")

    proc = subprocess.Popen(cmd, stdout=server_log_file, stderr=subprocess.STDOUT, text=True)

    ready = False
    start_load = time.time()
    for i in range(120):
        time.sleep(1)
        try:
            req = urllib.request.Request(f"http://127.0.0.1:{PORT}/health")
            with urllib.request.urlopen(req, timeout=2) as res:
                if res.status == 200:
                    ready = True
                    break
        except urllib.error.HTTPError as e:
            if e.code == 503 and i % 15 == 0:
                print(f"  Loading model... ({i}s elapsed)")
        except Exception:
            pass

        if proc.poll() is not None:
            print(f"ERROR: Server process died with code {proc.returncode}!")
            server_log_file.close()
            with open(server_log_path, "r", encoding="utf-8") as f:
                print("Server Log Tail:\n", "".join(f.readlines()[-20:]))
            return None

    load_time = round(time.time() - start_load, 2)
    if not ready:
        print("ERROR: Server timed out waiting for ready state!")
        proc.kill()
        server_log_file.close()
        return None

    print(f"Server READY in {load_time}s! Running generation benchmark...")

    monitor = SystemMonitor(interval=0.25)
    monitor.start()

    payload = {
        "model": "glm-4.7-flash",
        "messages": [{"role": "user", "content": PROMPT_A}],
        "max_tokens": max_gen_tokens,
        "temperature": 0.2,
        "stream": True,
        "stop": ["<|endoftext|>", "<|user|>", "<|observation|>", "<|im_end|>"]
    }

    req_data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(f"http://127.0.0.1:{PORT}/v1/chat/completions", data=req_data, headers={"Content-Type": "application/json"})

    req_start = time.time()
    first_token_time = None
    token_timestamps = []
    full_response = []
    timings_from_server = {}

    try:
        with urllib.request.urlopen(req, timeout=300) as response:
            for line in response:
                if not line:
                    continue
                line_str = line.decode("utf-8", errors="replace").strip()
                if not line_str or not line_str.startswith("data: "):
                    continue
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
                timings = chunk.get("timings")
                if timings:
                    timings_from_server = timings
    except Exception as e:
        print(f"Generation error: {e}")
    finally:
        req_end = time.time()
        monitor.stop()

    print("Shutting down server...")
    proc.terminate()
    try:
        proc.wait(timeout=5)
    except Exception:
        proc.kill()
    server_log_file.close()

    summary_metrics = monitor.get_summary()

    ttft_sec = round(first_token_time - req_start, 3) if first_token_time else 0.0
    total_sec = round(req_end - req_start, 3)
    gen_sec = round(req_end - first_token_time, 3) if first_token_time else total_sec

    num_tokens = len(token_timestamps)
    tok_per_sec = round(num_tokens / gen_sec, 2) if gen_sec > 0 else 0.0

    prompt_toks = timings_from_server.get("prompt_n", 0)
    prompt_ms = timings_from_server.get("prompt_ms", 0)
    prompt_tok_per_sec = round((prompt_toks / (prompt_ms / 1000.0)), 2) if prompt_ms > 0 else 0.0

    result = {
        "config_name": config_name,
        "engine": "llama.cpp (standalone CUDA 13)",
        "ngl": ngl if ngl is not None else "auto-fit",
        "flash_attn": flash_attn,
        "ctx": ctx,
        "threads": threads,
        "batch": batch,
        "ubatch": ubatch,
        "mmap": mmap,
        "load_time_sec": load_time,
        "ttft_sec": ttft_sec,
        "gen_sec": gen_sec,
        "total_sec": total_sec,
        "tokens_generated": num_tokens,
        "gen_tok_per_sec": tok_per_sec,
        "prompt_tokens": prompt_toks,
        "prompt_tok_per_sec": prompt_tok_per_sec,
        "system_metrics": summary_metrics,
        "timings_raw": timings_from_server
    }

    out_file = os.path.join(BENCH_DIR, f"{config_name}.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    print(f"FINISHED: tok/s = {tok_per_sec}, TTFT = {ttft_sec}s, Peak VRAM = {summary_metrics.get('peak_vram_mib')} MiB, Peak RAM = {summary_metrics.get('peak_ram_gb')} GB")
    print(f"Saved metrics to {out_file}")
    return result

if __name__ == "__main__":
    test_target = sys.argv[1] if len(sys.argv) > 1 else "autofit"
    
    if test_target == "autofit":
        run_test_configuration("llamacpp_autofit_fa_on", ngl=None, flash_attn=True, ctx=4096, threads=8, max_gen_tokens=256)
    elif test_target == "ngl24":
        run_test_configuration("llamacpp_ngl24_fa_on", ngl=24, flash_attn=True, ctx=4096, threads=8, max_gen_tokens=256)
    elif test_target == "ngl32":
        run_test_configuration("llamacpp_ngl32_fa_on", ngl=32, flash_attn=True, ctx=4096, threads=8, max_gen_tokens=256)
    elif test_target == "ngl40":
        run_test_configuration("llamacpp_ngl40_fa_on", ngl=40, flash_attn=True, ctx=4096, threads=8, max_gen_tokens=256)
    elif test_target == "ctx8k":
        run_test_configuration("llamacpp_ctx8k", ngl=None, flash_attn=True, ctx=8192, threads=8, max_gen_tokens=256)
    elif test_target == "ctx16k":
        run_test_configuration("llamacpp_ctx16k", ngl=None, flash_attn=True, ctx=16384, threads=8, max_gen_tokens=256)
    elif test_target == "ctx32k":
        run_test_configuration("llamacpp_ctx32k", ngl=None, flash_attn=True, ctx=32768, threads=8, max_gen_tokens=256)
    elif test_target == "ctx64k":
        run_test_configuration("llamacpp_ctx64k", ngl=None, flash_attn=True, ctx=65536, threads=8, max_gen_tokens=256)
    elif test_target == "ctx_suite":
        for c in [8192, 16384, 32768, 65536]:
            print(f"\n>>> Running Context Sweep: {c} tokens...")
            run_test_configuration(f"llamacpp_ctx_{c}", ngl=None, flash_attn=True, ctx=c, threads=8, max_gen_tokens=256)
    elif test_target == "thread_suite":
        for t in [6, 8, 12]:
            print(f"\n>>> Running Thread Sweep: {t} threads...")
            run_test_configuration(f"llamacpp_threads_{t}", ngl=None, flash_attn=True, ctx=8192, threads=t, max_gen_tokens=256)
    elif test_target == "batch_suite":
        for b, ub in [(512, 256), (512, 512), (1024, 512), (2048, 512)]:
            print(f"\n>>> Running Batch Sweep: b={b}, ub={ub}...")
            run_test_configuration(f"llamacpp_b{b}_ub{ub}", ngl=None, flash_attn=True, ctx=8192, threads=8, batch=b, ubatch=ub, max_gen_tokens=256)

