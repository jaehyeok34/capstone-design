import requests

res = requests.get('http://localhost:1780/csv/cardinality/data/name')

if res.status_code == 200:
    print(res.json())

print(res.text)
