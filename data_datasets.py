import requests

res = requests.get('http://localhost:1780/data/datasets')
if res.status_code != 200:
    print(res.text)
    exit(1)

print(res.json())