from dataclasses import dataclass
from flask import Flask
import pandas as pd
from flask import request
import hashlib
from typing import Dict, List

from api_gateway import subscribe, request_get_column_data, request_update_csv

@dataclass
class PyMatchingKeyDTO:
    piiColumns: Dict[str, List[str]] # key: title, value: column


app = Flask(__name__)

@app.route('/')
def home():
    return "matching-key-server"


@app.route('/generate-matching-key', methods=['POST'])
def generate_matching_key():
    try:
        data = PyMatchingKeyDTO(**request.get_json())
    except TypeError as _:
        return "[debug] selectedRegisteredDataTitle과 piiColumns가 있어야 함", 400
    
    # TODO: piiColumns에 해당하는 데이터 가져오기
    df_list = {}
    for title, columns in data.piiColumns.items():
        df_list[title] = request_get_column_data(
            selectedRegisteredDataTitle=title,
            columns=columns
        )

    def hash_row(row):
        concatenated = "".join(row.astype(str))
        return hashlib.sha256(concatenated.encode('utf-8')).hexdigest()
    

    for title, df in df_list.items():
        df['matching_key'] = df.apply(hash_row, axis=1)
        df['mk_serial_number'] = [f"{title}{i+1}" for i in range(len(df))]

    for title, df in df_list.items():
        df.to_csv(f'"{title}".csv', index=False)
        request_update_csv(
            selectedRegisteredDataTitle=title,
            df=df
        )

    print("[debug] matching key generated")
    return "", 200
    

if __name__ == '__main__':
    port = 1783
    callback_url = f'http://localhost:{port}/generate-matching-key'

    ok = subscribe(
        topic='matching-key.generate.request',
        callback_url=callback_url,
        count=3,
        interval=5
    )
    if not ok:
        exit(1)

    # app.run(port=port, debug=True)
    app.run(port=port)