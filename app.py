from flask import Flask
import requests
from typing import Dict, List
import pandas as pd

app = Flask(__name__)

@app.route('/')
def home():
    data = get_data()
    columns = list(data.keys())
    print("[debug] columns:", columns)

    identities = get_identity(columns)["identity"]
    print("[debug] identities:", identities)

    df = pd.DataFrame(data)
    new_dataframe = df[identities].copy()
    print("[debug] new_dataframe:", new_dataframe)

    matching_key = generate_matching_key(new_dataframe)
    print("[debug] matching_key:", matching_key)
    pd.DataFrame(matching_key).to_csv("matching_key.csv", index=False)

    return identities


def get_data():
    response = requests.get('http://127.0.0.1:1781/get-data')

    if response.status_code != 200:
        return "Failed to fetch data", 500

    data: Dict = response.json()
    if not data:
        return "No data available", 500
    
    return data


def get_identity(payload: List[str]):
    url = 'http://127.0.0.1:1782/detect'
    headers = {'Content-Type': 'application/json'}

    response = requests.post(url, json=payload, headers=headers)

    if response.status_code != 200:
        return "Failed to identify", 500

    return response.json()


def generate_matching_key(data: pd.DataFrame):
    response = requests.post('http://127.0.0.1:1783/gen-matching-key', json=data.to_dict())
    if response.status_code != 200:
        return "Failed to generate matching key", 500
    
    return response.json()

if __name__ == '__main__':
    app.run(port="1780", debug=True)