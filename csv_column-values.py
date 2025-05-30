import requests

# datasetInfo = "datas.csv" # 없는 데이터셋
datasetInfo = "data1_20250529213309454778.csv" # 있는 데이터셋
res = requests.post(
    'http://localhost:1780/csv/column-values/' + datasetInfo, 
    json=["name", "ssn"]
)

if res.status_code != 200:
    print(res.text)
    exit(1)

print(res.json())