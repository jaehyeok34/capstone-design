import pandas as pd
import requests

# 결합키 생성 루틴 요청
url = "http://localhost:1780/matching-key-routine-request"

datas = ["datas.csv"]
response = requests.post(url, json=datas)
print(response.status_code)