import subprocess
import time
import urllib.request
import json
import os

SERVER_EXE = r"D:\AI\llama.cpp\bin-ollama\llama-server.exe"
MODEL_PATH = r"D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf"
PORT = 8080

def test_server():
    cmd = [
        SERVER_EXE,
        "--model", MODEL_PATH,
        "--port", str(PORT),
        "--host", "127.0.0.1",
        "-ngl", "48",
        "-c", "4096",
        "--flash-attn", "auto",
        "-t", "8",
        "--no-warmup"
    ]
    print(f"Starting server: {' '.join(cmd)}")
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    
    # Wait for ready
    ready = False
    for _ in range(60):
        time.sleep(1)
        try:
            req = urllib.request.Request(f"http://127.0.0.1:{PORT}/health")
            with urllib.request.urlopen(req, timeout=2) as res:
                if res.status == 200:
                    print("Server is healthy!")
                    ready = True
                    break
        except Exception:
            pass
        if proc.poll() is not None:
            print("Process exited prematurely with code:", proc.returncode)
            out, _ = proc.communicate()
            print("Output:\n", out[:1500])
            return

    if ready:
        print("Checking /v1/models...")
        try:
            req = urllib.request.Request(f"http://127.0.0.1:{PORT}/v1/models")
            with urllib.request.urlopen(req, timeout=2) as res:
                data = json.loads(res.read())
                print("Models endpoint response:", json.dumps(data, indent=2))
        except Exception as e:
            print("Models check error:", e)

    print("Stopping server...")
    proc.terminate()
    try:
        proc.wait(timeout=5)
    except Exception:
        proc.kill()
    print("Server stopped cleanly.")

if __name__ == "__main__":
    test_server()
