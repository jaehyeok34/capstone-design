from flask import Flask
import pandas as pd
from flask import request, jsonify
import hashlib
import requests
from typing import Dict, List, Any

app = Flask(__name__)

@app.route('/')
def home():
    return "matching-key-server"


@app.route('/gen-matching-key', methods=['POST'])
def generate_matching_key():
    data: Dict[str, Dict[str, List[Any]]] = request.get_json()
    if (
        not data or
        'pii-data' not in data or
        not isinstance(data['pii-data'], dict)
    ):
        return jsonify({'error': '올바르지 않은 요청입니다. pii-data를 포함해야 합니다.'}), 400
    
    # JSON 데이터에서 DataFrame 생성
    try:
        df = pd.DataFrame(data['pii-data'])

    except ValueError as e:
        return jsonify({'error': '올바르지 않은 JSON 형식입니다.'}), 400
    
    def hash_row(row):
        concatenated = "".join(row.astype(str))
        return hashlib.sha256(concatenated.encode('utf-8')).hexdigest()

    df['matching_key'] = df.apply(hash_row, axis=1)

    return df.to_json(orient='records'), 200
    

if __name__ == '__main__':
    

    app.run(port=1783, debug=True)