import requests


url = "http://localhost:1780/matching-key-routine-request"

datas = ["datas.csv"]
response = requests.post(url, json=datas)
print(response.status_code)