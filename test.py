import requests

res = requests.get('http://localhost:1789/csv/data/name/cardinality')

if res.status_code == 200:
    print(res.json())

print(res.text)
