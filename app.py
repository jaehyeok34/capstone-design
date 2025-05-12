from flask import Flask
import requests
from typing import Dict, List

app = Flask(__name__)

@app.route('/')
def home():
    data = get_data()
    columns = list(data.keys())

    print(columns)
    identity = get_identity(columns)
    print(identity)
    return "good"

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

if __name__ == '__main__':
    app.run(port="1780", debug=True)