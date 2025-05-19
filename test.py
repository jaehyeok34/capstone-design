
import pandas as pd
import requests

df = pd.read_csv('"datas.csv".csv')
print(df)

res = requests.post(
    url="http://127.0.0.1:1780/update-csv", 
    json={
        "selectedRegisteredDataTitle": "datas.csv",
        "matchingKeyData": df.to_dict()
    }
)

print(res.status_code)
print(res.text)