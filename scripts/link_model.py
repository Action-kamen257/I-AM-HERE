import os
import ctypes

BLOB_PATH = r"D:\AI\Ollama\models\blobs\sha256-9eba2761cf0b88b8bc11a065a7b5b47f1b13ce820e8e492cb1010b450f9ec950"
TARGET_DIR = r"D:\AI\GLM-4.7-Flash\models"
TARGET_FILE = os.path.join(TARGET_DIR, "glm-4.7-flash-q4_K_M.gguf")

def main():
    os.makedirs(TARGET_DIR, exist_ok=True)
    if os.path.exists(TARGET_FILE):
        print(f"File already exists: {TARGET_FILE} ({os.path.getsize(TARGET_FILE)/(1024**3):.2f} GB)")
        return TARGET_FILE

    print(f"Creating NTFS hardlink from:\n  {BLOB_PATH}\nto:\n  {TARGET_FILE}")
    res = ctypes.windll.kernel32.CreateHardLinkW(TARGET_FILE, BLOB_PATH, None)
    if res:
        print(f"Hardlink created successfully! Size: {os.path.getsize(TARGET_FILE)/(1024**3):.2f} GB (0 bytes extra disk used)")
        return TARGET_FILE
    else:
        err = ctypes.GetLastError()
        print(f"Failed to create hardlink (Error {err}). Falling back to direct path.")
        return BLOB_PATH

if __name__ == "__main__":
    main()
