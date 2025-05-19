from dataclasses import dataclass
from typing import Dict, List
from flask import Flask, request
import pandas as pd
from flask import jsonify
import os

@dataclass
class PyColumnDataDTO:
    csvFilePath: str
    columns: List[str]


@dataclass
class PyUpdateCSVDTO:
    csvFilePath: str
    matchingKeyData: Dict[str, List[str]]


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


@app.route('/update-csv', methods=['POST'])
def update_csv():
    try:
        data = PyUpdateCSVDTO(**request.get_json())
    except TypeError as _:
        return '[debug] "csvFilePath" and "matchingKeyData"이 없음', 400
    
    # data.csvFilePath에 있는 csv 파일을 dataframe으로 변환
    origin = pd.read_csv(data.csvFilePath)
    matching_key_data = pd.DataFrame(data.matchingKeyData)

    # 공통 컬럼(b) 기준으로 origin과 mkd를 병합
    common_cols = list(set(origin.columns) & set(matching_key_data.columns))
    if not common_cols:
        return '[debug] 공통 컬럼이 없음', 400

    # 병합 (left join)
    merged = pd.merge(origin, matching_key_data, on=common_cols, how='left')

    # csv 파일로 저장 (새로 만들기)
    dir_name = os.path.dirname(data.csvFilePath)
    base_name = os.path.basename(data.csvFilePath)
    new_file_name = f"mk_{base_name}"

    new_file_path = os.path.join(dir_name, new_file_name)
    merged.to_csv(new_file_path, index=False)

    print(f"[debug] {new_file_path}에 저장됨")

    return "", 200


if __name__ == '__main__':
    port = 1789

    # app.run(port=port, debug=True)
    app.run(port=port)