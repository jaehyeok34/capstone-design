import pandas as pd
import requests

# # def request():
# #     url = "http://localhost:1789/get-pii-data"

# #     response = requests.get(url)
# #     if response.status_code == 200:
# #         print("Success:", response.json().keys())
# #         pd.DataFrame(response.json()).to_csv('datas2.csv', index=False)
# #     else:
# #         print("Error:", response.status_code)

# # request()

# # csv 업로드
# url = "http://localhost:1780/upload-csv"
# files = {'file': open('datas.csv', 'rb')}
# response = requests.post(url, files=files)

# print(response.status_code)


# 결합키 생성 루틴 요청
# url = "http://localhost:1780/matching-key-routine-request"

# datas = ["datas.csv"]
# response = requests.post(url, json=datas)
# print(response.status_code)

# 컬럼 얻어오기
# url = "http://localhost:1780/get-columns"
# data = "datas.csv"
# res = requests.post(url, data=data)
# print(res.status_code)
# print(res.text)


# matching-key-routine-request -> 

data = {
    "title": "datas.csv",
    "columns": ["name", "ssn", "email"]
}
response = requests.post("http://127.0.0.1:1780/get-column-data", json=data)
print(response.status_code)

df = pd.DataFrame(response.json())
print(df)
