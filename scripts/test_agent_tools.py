import os
import sys
import time
import json
import re
import subprocess
import urllib.request
import urllib.error

SERVER_EXE = r"D:\AI\llama.cpp\bin-ollama\llama-server.exe"
MODEL_PATH = r"D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf"
LOG_DIR = r"D:\AI\Antigravity-Local\logs"
BENCH_DIR = r"D:\AI\Antigravity-Local\benchmarks"
PORT = 8080
ENDPOINT = f"http://127.0.0.1:{PORT}/v1/chat/completions"
MODEL = "glm-4.7-flash"

TOOLS_SCHEMA = [
    {
        "type": "function",
        "function": {
            "name": "run_terminal_command",
            "description": "Execute a shell or PowerShell command on the system and return the output.",
            "parameters": {
                "type": "object",
                "properties": {
                    "command": {
                        "type": "string",
                        "description": "The command line string to execute."
                    }
                },
                "required": ["command"]
            }
        }
    }
]

def check_server_ready():
    try:
        req = urllib.request.Request(f"http://127.0.0.1:{PORT}/health")
        with urllib.request.urlopen(req, timeout=2) as res:
            return res.status == 200
    except Exception:
        return False

def parse_glm_tool_calls(content):
    """Parses GLM native XML function calls if returned in content."""
    tool_calls = []
    invoke_pattern = re.compile(r'<invoke name="([^"]+)">\s*(.*?)\s*</invoke>', re.DOTALL)
    param_pattern = re.compile(r'<parameter_argument name="([^"]+)">\s*(.*?)\s*</parameter_argument>', re.DOTALL)

    for match in invoke_pattern.finditer(content):
        fn_name = match.group(1)
        body = match.group(2)
        params = {}
        for p in param_pattern.finditer(body):
            params[p.group(1)] = p.group(2).strip()
        tool_calls.append({
            "id": f"call_{len(tool_calls)+1}",
            "type": "function",
            "function": {
                "name": fn_name,
                "arguments": json.dumps(params)
            }
        })
    return tool_calls

def test_tool_calling():
    os.makedirs(LOG_DIR, exist_ok=True)
    os.makedirs(BENCH_DIR, exist_ok=True)

    proc = None
    if not check_server_ready():
        print("Starting llama-server with --chat-template chatglm4...")
        log_file = open(os.path.join(LOG_DIR, "tools_server.log"), "w", encoding="utf-8")
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
        proc = subprocess.Popen(cmd, stdout=log_file, stderr=subprocess.STDOUT, text=True)
        for i in range(120):
            time.sleep(1)
            if check_server_ready():
                print(f"Server ready in {i+1}s!")
                break
            if proc.poll() is not None:
                print("Server failed to start!")
                return False

    print("\n=======================================================")
    print("TESTING ANTIGRAVITY TOOL CALLING & AGENT EXECUTION")
    print("=======================================================")

    messages = [
        {"role": "system", "content": "You are Antigravity, an expert AI coding assistant. You have access to tools. When you need system information, call the appropriate tool."},
        {"role": "user", "content": "Please check the available free disk space on drive D: using the terminal command tool."}
    ]

    payload = {
        "model": MODEL,
        "messages": messages,
        "tools": TOOLS_SCHEMA,
        "tool_choice": "auto",
        "temperature": 0.1,
        "max_tokens": 512
    }

    req_data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(ENDPOINT, data=req_data, headers={"Content-Type": "application/json"})

    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"Error calling completion endpoint: {e}")
        if proc:
            proc.terminate()
        return False

    elapsed = round(time.time() - t0, 3)
    choice = data.get("choices", [{}])[0]
    msg = choice.get("message", {})
    raw_content = msg.get("content", "")
    tool_calls = msg.get("tool_calls", [])

    print(f"Turn 1 Response received in {elapsed}s:")
    print(f"Role: {msg.get('role')}")
    print(f"Content: {raw_content}")

    if not tool_calls and ("<invoke" in raw_content or "<function_calls>" in raw_content):
        print("Detected GLM XML function call syntax in content! Parsing structured tool call...")
        tool_calls = parse_glm_tool_calls(raw_content)

    print(f"Structured Tool Calls: {json.dumps(tool_calls, indent=2)}")

    if not tool_calls:
        print("FAIL: Model did not generate any tool call.")
        if proc:
            proc.terminate()
        return False

    call = tool_calls[0]
    call_id = call.get("id", "call_1")
    fn_name = call.get("function", {}).get("name")
    fn_args = call.get("function", {}).get("arguments", "{}")

    print(f"\nSimulating tool response for: {fn_name}({fn_args})...")
    messages.append({
        "role": "assistant",
        "content": raw_content,
        "tool_calls": tool_calls
    })
    messages.append({
        "role": "tool",
        "tool_call_id": call_id,
        "name": fn_name,
        "content": "FreeSpace=297854636032\nSize=509521616896"
    })

    # Step 2: Final answer after tool execution
    payload_step2 = {
        "model": MODEL,
        "messages": messages,
        "tools": TOOLS_SCHEMA,
        "temperature": 0.1,
        "max_tokens": 256
    }
    req_data2 = json.dumps(payload_step2).encode("utf-8")
    req2 = urllib.request.Request(ENDPOINT, data=req_data2, headers={"Content-Type": "application/json"})

    t1 = time.time()
    try:
        with urllib.request.urlopen(req2, timeout=120) as resp2:
            data2 = json.loads(resp2.read().decode("utf-8"))
    except Exception as e:
        print(f"Error in step 2: {e}")
        if proc:
            proc.terminate()
        return False

    elapsed2 = round(time.time() - t1, 3)
    msg2 = data2.get("choices", [{}])[0].get("message", {})
    final_content = msg2.get("content", "")
    print(f"\nTurn 2 (Final Agent Response) received in {elapsed2}s:")
    print(final_content)

    verification_result = {
        "benchmark": "antigravity_tool_calling_verification",
        "model": MODEL,
        "turn_1_ttft_sec": elapsed,
        "tool_call_generated": True,
        "tool_called_name": fn_name,
        "tool_called_arguments": fn_args,
        "turn_2_duration_sec": elapsed2,
        "final_response": final_content,
        "success": True
    }

    out_file = os.path.join(BENCH_DIR, "tool_calling_verification.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(verification_result, f, indent=2)

    print(f"\nSUCCESS: Complete tool-call round trip verified! Saved to {out_file}")

    if proc:
        print("Shutting down test server...")
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except Exception:
            proc.kill()

    return True

if __name__ == "__main__":
    success = test_tool_calling()
    sys.exit(0 if success else 1)
