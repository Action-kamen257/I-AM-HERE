import urllib.request
import json

url = 'https://api.github.com/repos/ggml-org/llama.cpp/releases/latest'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as res:
        data = json.loads(res.read())
        print('Latest llama.cpp release tag:', data.get('tag_name'))
        for a in data.get('assets', []):
            name = a.get('name', '')
            if 'win' in name.lower() and 'cuda' in name.lower():
                sz = round(a.get('size', 0) / (1024*1024), 1)
                dl = a.get('browser_download_url')
                print(f"{name} ({sz} MB): {dl}")
except Exception as e:
    print('Error:', e)
