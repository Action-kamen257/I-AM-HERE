#!/usr/bin/env python3
"""
Interactive Streaming Terminal Chat with Local GLM-4.7-Flash
Optimized for low-latency coding assistance, codebase awareness, and clean terminal output.
"""

import sys
import os
import json
import time
import urllib.request
import urllib.error

ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"
FALLBACK_ENDPOINT = "http://127.0.0.1:11434/v1/chat/completions"

STOP_TOKENS = [
    "<|im_end|>",
    "<|im_start|>",
    "<|user|>",
    "<|assistant|>",
    "<|endoftext|>"
]

WORKSPACE_SUMMARY = """WORKSPACE: I_AM_HERE (Emergency SOS & Personal Safety Ecosystem)
Location: d:/tushar/I_AM_HERE

Key Modules:
1. android/: Native Android Kotlin app:
   - Location & Sensor: ShakeDetector.kt, MotionFeatureExtractor.kt, FakeCallManager.kt, JourneyManager.kt
   - Services & SMS: EmergencyLocationService.kt, SmsStatusReceiver.kt, SosWidgetProvider.kt
2. backend/: Node.js & TypeScript Express + WebSocket server:
   - Core Store: src/store.ts (EmergencyStore session lifecycle, PIN verification, location breadcrumbs)
   - API Routes: src/app.ts (SOS triggers, phone login, contact status, auto-update version endpoint)
   - Gateways: src/smsGateway.ts, src/push.ts, src/partnerships.ts (EDXL-CAP alerts)
   - Unit Tests: src/__tests__/ (13 suites, 55 automated Jest tests)
3. scripts/: AI benchmarking, local GLM-4.7-Flash orchestration, model routing (model_router.py, verifier.py).
4. docs/: Phases 00 to 16 full architectural specification markdown files."""

SYSTEM_PROMPT = f"""You are GLM-4.7-Flash, an expert AI software engineer running locally on the user's NVIDIA GeForce RTX 5060 Laptop GPU.
You are directly assisting the user on their project.

{WORKSPACE_SUMMARY}

Guidelines:
- Provide direct, concise, and production-quality answers and code.
- You understand the entire I_AM_HERE project structure, including the Android app, TypeScript backend, and Python scripts.
- Never hallucinate future conversation turns or output special control tags."""


def check_endpoint(url: str) -> bool:
    try:
        check_url = url.replace("/chat/completions", "/models")
        req = urllib.request.Request(check_url, headers={"User-Agent": "GLM-Chat/1.0"})
        with urllib.request.urlopen(req, timeout=1.5) as res:
            return res.status == 200
    except Exception:
        return False


def get_gpu_status():
    try:
        import subprocess
        out = subprocess.check_output(
            ["nvidia-smi", "--query-gpu=memory.used,memory.total,temperature.gpu,utilization.gpu", "--format=csv,noheader,nounits"],
            text=True
        ).strip()
        parts = [p.strip() for p in out.split(",")]
        return f"{parts[0]} / {parts[1]} MiB VRAM | Temp: {parts[2]}°C | GPU Util: {parts[3]}%"
    except Exception:
        return "NVIDIA RTX 5060 Laptop GPU"


def stream_chat(messages, endpoint):
    payload = {
        "model": "GLM-4.7-Flash",
        "messages": messages,
        "temperature": 0.2,
        "presence_penalty": 0.1,
        "frequency_penalty": 0.1,
        "max_tokens": 1024,
        "stop": STOP_TOKENS,
        "stream": True
    }

    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        endpoint,
        data=data,
        headers={"Content-Type": "application/json"}
    )

    full_response = []
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            for line in response:
                line = line.decode("utf-8", errors="replace").strip()
                if line.startswith("data: "):
                    content = line[6:].strip()
                    if content == "[DONE]":
                        break
                    try:
                        chunk = json.loads(content)
                        delta = chunk.get("choices", [{}])[0].get("delta", {}).get("content")
                        if delta:
                            # Filter out any stray stop tokens that might slip through
                            for st in STOP_TOKENS:
                                if st in delta:
                                    delta = delta.replace(st, "")
                            if delta:
                                sys.stdout.write(delta)
                                sys.stdout.flush()
                                full_response.append(delta)
                    except Exception:
                        pass
        print()
        return "".join(full_response).strip()
    except Exception as e:
        print(f"\n[Error communicating with model: {e}]")
        return None


def print_help():
    print("""
Available Commands:
  /help     - Show this help message
  /clear    - Reset conversation history
  /status   - Display GPU VRAM and local server health
  /files    - List key codebase files
  exit / q  - Exit the chat session
""")


def print_files():
    print("""
Key Project Files in d:\\tushar\\I_AM_HERE:
  Backend:
    - backend/src/store.ts              (EmergencyStore state & PIN handling)
    - backend/src/app.ts                (REST API routes & WebSocket server)
    - backend/src/__tests__/store.test.ts (Jest unit tests for sessionStore)
  Android:
    - android/app/src/main/java/.../sensor/ShakeDetector.kt (Shake detection)
    - android/app/src/main/java/.../service/SmsStatusReceiver.kt (SMS carrier tracking)
    - android/app/src/main/java/.../safety/JourneyManager.kt (Walk-with-me companion)
  Model Orchestration:
    - scripts/model_router.py           (Automatic model routing & dispatch)
    - scripts/verifier.py               (Autonomous QA & verification runner)
""")


def main():
    target_endpoint = ENDPOINT if check_endpoint(ENDPOINT) else FALLBACK_ENDPOINT
    port_label = "Port 8080 (llama-server)" if "8080" in target_endpoint else "Port 11434 (ollama)"
    gpu_info = get_gpu_status()

    print("=" * 65)
    print("  LOCAL GLM-4.7-FLASH CODEBASE ASSISTANT")
    print(f"  Endpoint: {port_label}")
    print(f"  Hardware: {gpu_info}")
    print("  Context:  I_AM_HERE Workspace Active")
    print("  Commands: Type '/help' for commands, 'exit' or 'q' to quit.")
    print("=" * 65)
    print()

    messages = [{"role": "system", "content": SYSTEM_PROMPT}]

    while True:
        try:
            user_input = input("You > ").strip()
            if not user_input:
                continue

            # Command handling
            cmd = user_input.lower()
            if cmd in ["exit", "quit", "q"]:
                print("\nEnding session. Happy coding!")
                break
            elif cmd == "/help":
                print_help()
                continue
            elif cmd == "/clear":
                messages = [{"role": "system", "content": SYSTEM_PROMPT}]
                print("\n[Conversation history cleared. Workspace context preserved.]\n")
                continue
            elif cmd == "/status":
                print(f"\n[GPU Status: {get_gpu_status()}]")
                print(f"[Endpoint: {target_endpoint} - ONLINE]\n")
                continue
            elif cmd == "/files":
                print_files()
                continue

            messages.append({"role": "user", "content": user_input})
            print("\nGLM > ", end="", flush=True)

            reply = stream_chat(messages, target_endpoint)
            if reply:
                messages.append({"role": "assistant", "content": reply})
            print()

        except (KeyboardInterrupt, EOFError):
            print("\n\nSession terminated.")
            break


if __name__ == "__main__":
    main()
