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

TURNS = [
    "Analyze the architectural design of an asynchronous distributed LRU cache with TTL in Python. List key considerations.",
    "Now write the Python class `AsyncLRUCache` implementing `get`, `put`, `delete`, and `evict_expired` using an OrderedDict and lock.",
    "Add a background cleanup task `_auto_eviction_loop` that periodically wakes up and prunes expired keys without blocking callers.",
    "Provide a suite of pytest tests verifying concurrent access, TTL expiration, and LRU eviction order under heavy concurrent async load.",
    "Refactor the cache to support an optional secondary fallback store (e.g. disk or remote storage) when a key is evicted or misses."
]

def run_stress_session():
    os.makedirs(LOG_DIR, exist_ok=True)
    os.makedirs(BENCH_DIR, exist_ok=True)

    log_path = os.path.join(LOG_DIR, "stress_test_server.log")
    log_file = open(log_path, "w", encoding="utf-8")

    cmd = [
        SERVER_EXE,
        "--model", MODEL_PATH,
        "--port", str(PORT),
        "--host", "127.0.0.1",
        "-c", "16384",
        "-b", "512",
        "-ub", "512",
        "-t", "8",
        "-np", "1",
        "-fit", "on",
        "--flash-attn", "on",
        "--no-warmup"
    ]

    print("\n=======================================================")
    print("STARTING LONG-SESSION CODING AGENT STRESS TEST")
    print(f"Server CMD: {' '.join(cmd)}")
    print("=======================================================")

    proc = subprocess.Popen(cmd, stdout=log_file, stderr=subprocess.STDOUT, text=True)

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
        except Exception:
            pass

        if proc.poll() is not None:
            print("ERROR: Server died during startup!")
            log_file.close()
            return

    load_time = round(time.time() - start_load, 2)
    print(f"Server READY in {load_time}s! Executing multi-turn agent workflow...")

    conversation = [
        {"role": "system", "content": "You are Antigravity, an expert software engineering assistant. Write clean, production-ready Python code."}
    ]

    turn_metrics = []
    monitor = SystemMonitor(interval=0.5)
    monitor.start()

    session_start = time.time()

    for idx, prompt_text in enumerate(TURNS, start=1):
        print(f"\n--- Running Turn {idx}/{len(TURNS)} ---")
        conversation.append({"role": "user", "content": prompt_text})

        payload = {
            "model": "glm-4.7-flash",
            "messages": conversation,
            "max_tokens": 300,
            "temperature": 0.2,
            "stream": True,
            "stop": ["<|endoftext|>", "<|user|>", "<|observation|>", "<|im_end|>"]
        }

        req_data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(f"http://127.0.0.1:{PORT}/v1/chat/completions", data=req_data, headers={"Content-Type": "application/json"})

        turn_start = time.time()
        first_token_time = None
        token_count = 0
        response_chunks = []
        server_timings = {}

        try:
            with urllib.request.urlopen(req, timeout=180) as resp:
                for line in resp:
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
                        response_chunks.append(piece)
                        token_count += 1
                    timings = chunk.get("timings")
                    if timings:
                        server_timings = timings
        except Exception as e:
            print(f"Error on turn {idx}: {e}")

        turn_end = time.time()
        assistant_reply = "".join(response_chunks)
        conversation.append({"role": "assistant", "content": assistant_reply})

        ttft = round(first_token_time - turn_start, 3) if first_token_time else 0.0
        gen_duration = round(turn_end - first_token_time, 3) if first_token_time else round(turn_end - turn_start, 3)
        tok_s = round(token_count / gen_duration, 2) if gen_duration > 0 else 0.0

        print(f"  Turn {idx} Complete: {token_count} tokens in {gen_duration}s -> {tok_s} tok/s (TTFT: {ttft}s)")

        turn_metrics.append({
            "turn": idx,
            "prompt_length_chars": len(prompt_text),
            "response_tokens": token_count,
            "ttft_sec": ttft,
            "gen_sec": gen_duration,
            "gen_tok_per_sec": tok_s,
            "server_timings": server_timings
        })

    session_duration = round(time.time() - session_start, 2)
    monitor.stop()

    print("\nShutting down server...")
    proc.terminate()
    try:
        proc.wait(timeout=5)
    except Exception:
        proc.kill()
    log_file.close()

    summary = monitor.get_summary()

    report = {
        "benchmark": "multi_turn_agent_stress_session",
        "total_duration_sec": session_duration,
        "turns_completed": len(turn_metrics),
        "turn_results": turn_metrics,
        "system_metrics_overall": summary
    }

    out_file = os.path.join(BENCH_DIR, "stress_test_session.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    print(f"\nSTRESS TEST COMPLETE in {session_duration}s!")
    print(f"Overall Peak VRAM: {summary.get('peak_vram_mib')} MiB")
    print(f"Overall Peak RAM: {summary.get('peak_ram_gb')} GB")
    print(f"Overall Peak Pagefile: {summary.get('peak_pagefile_gb')} GB")
    print(f"Saved stress test results to: {out_file}")

if __name__ == "__main__":
    run_stress_session()
