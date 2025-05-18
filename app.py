from dataclasses import dataclass
from typing import List
from flask import Flask, request
import pandas as pd
from flask import jsonify

@dataclass
class PyColumnDataDTO:
    csvFilePath: str
    columns: List[str]

app = Flask(__name__)

@app.route('/')
def home():
    return "data-server"


@app.route('/get-column-data', methods=['POST'])
def get_column_data():
    print("[debug] get_column_data")
    try:
        data = PyColumnDataDTO(**request.get_json())
    except TypeError as _:
        return '[debug] "csvFilePath" and "columns"이 없음', 400
    
    df = pd.read_csv(data.csvFilePath)
    return jsonify(df[data.columns].to_dict()), 200


if __name__ == '__main__':
    port = 1789

    # app.run(port=port, debug=True)
    app.run(port=port)