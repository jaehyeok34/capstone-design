import requests

datasetInfo = "data1_20250529214939569951.csv"
res = requests.get('http://localhost:1780/data/columns/' + datasetInfo)
if res.status_code != 200:
    print(res.text)
    exit(1)

print(res.json())