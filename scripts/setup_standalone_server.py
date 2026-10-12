import os
import shutil
import subprocess

SRC_DIR = r"D:\AI\Ollama\bin\lib\ollama"
CUDA13_DIR = os.path.join(SRC_DIR, "cuda_v13")
DEST_DIR = r"D:\AI\llama.cpp\bin-ollama"

def main():
    os.makedirs(DEST_DIR, exist_ok=True)
    # Copy all files from SRC_DIR (excluding subdirectories)
    for f in os.listdir(SRC_DIR):
        p = os.path.join(SRC_DIR, f)
        if os.path.isfile(p):
            shutil.copy2(p, os.path.join(DEST_DIR, f))
    
    # Copy CUDA 13 ggml-cuda.dll directly into DEST_DIR
    cuda_dll = os.path.join(CUDA13_DIR, "ggml-cuda.dll")
    if os.path.exists(cuda_dll):
        shutil.copy2(cuda_dll, os.path.join(DEST_DIR, "ggml-cuda.dll"))
        print(f"Copied {cuda_dll} -> {DEST_DIR}\\ggml-cuda.dll")

    # Also copy cublas from D:\AI\llama.cpp\bin if needed
    for cb in ["cublas64_13.dll", "cublasLt64_13.dll", "cudart64_13.dll"]:
        cb_src = os.path.join(r"D:\AI\llama.cpp\bin", cb)
        if os.path.exists(cb_src):
            shutil.copy2(cb_src, os.path.join(DEST_DIR, cb))
            print(f"Copied {cb} -> {DEST_DIR}")

    # Test running llama-server.exe to check GPU detection!
    server_exe = os.path.join(DEST_DIR, "llama-server.exe")
    print(f"Testing {server_exe} GPU discovery...")
    try:
        out = subprocess.check_output(
            [server_exe, "--model", r"D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf", "-ngl", "1", "--no-warmup", "-c", "512", "--port", "8999"],
            stderr=subprocess.STDOUT,
            text=True,
            timeout=10
        )
        print("Output:", out[:1000])
    except subprocess.TimeoutExpired as e:
        print("Process timed out (which means it started successfully and is listening!):")
        print(e.output[:1000] if e.output else "Started listening!")
    except subprocess.CalledProcessError as e:
        print("Error output:\n", e.output[:1000])

if __name__ == "__main__":
    main()
