import pandas as pd
import requests

def request():
    url = "http://localhost:1789/get-pii-data"

    response = requests.get(url)
    if response.status_code == 200:
        print("Success:", response.json().keys())
        pd.DataFrame(response.json()).to_csv('datas2.csv', index=False)
    else:
        print("Error:", response.status_code)

request()