import os
import urllib.request
import zipfile
import shutil
import subprocess

DOWNLOAD_DIR = r"D:\AI\llama.cpp\download"
BIN_DIR = r"D:\AI\llama.cpp\bin"

URLS = [
    "https://github.com/ggerganov/llama.cpp/releases/download/b11318/llama-b11318-bin-win-cuda-13.4-x64.zip",
    "https://github.com/ggerganov/llama.cpp/releases/download/b11318/cudart-llama-bin-win-cuda-13.4-x64.zip"
]

def download_file(url, target_path):
    print(f"Downloading {url} -> {target_path}...")
    headers = {"User-Agent": "Mozilla/5.0"}
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as response, open(target_path, "wb") as out_file:
        total_size = int(response.headers.get("content-length", 0))
        downloaded = 0
        chunk_size = 1024 * 1024
        while True:
            chunk = response.read(chunk_size)
            if not chunk:
                break
            out_file.write(chunk)
            downloaded += len(chunk)
            if total_size > 0:
                percent = (downloaded / total_size) * 100
                print(f"\r  {downloaded / (1024*1024):.1f} MB / {total_size / (1024*1024):.1f} MB ({percent:.1f}%)", end="", flush=True)
        print()

def main():
    os.makedirs(DOWNLOAD_DIR, exist_ok=True)
    os.makedirs(BIN_DIR, exist_ok=True)

    for url in URLS:
        fname = url.split("/")[-1]
        zip_path = os.path.join(DOWNLOAD_DIR, fname)
        if not os.path.exists(zip_path) or os.path.getsize(zip_path) < 1000000:
            download_file(url, zip_path)
        else:
            print(f"Already downloaded: {zip_path} ({os.path.getsize(zip_path)/(1024*1024):.1f} MB)")

        print(f"Extracting {fname} to {BIN_DIR}...")
        with zipfile.ZipFile(zip_path, "r") as z:
            z.extractall(BIN_DIR)

    # Check llama-server.exe
    server_exe = os.path.join(BIN_DIR, "llama-server.exe")
    if os.path.exists(server_exe):
        print(f"SUCCESS: {server_exe} exists!")
        try:
            out = subprocess.check_output([server_exe, "--version"], text=True, stderr=subprocess.STDOUT)
            print("llama-server version output:\n", out.strip())
        except Exception as e:
            print("Error running --version:", e)
    else:
        print(f"ERROR: {server_exe} not found after extraction!")

if __name__ == "__main__":
    main()
