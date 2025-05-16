from flask import Flask
import pandas as pd
from flask import request, jsonify
import hashlib
from typing import Dict, Any
from api_gateway import subscribe, request_post

app = Flask(__name__)

@app.route('/')
def home():
    return "matching-key-server"

"""
input: {
    "columns": ["v1", "v2", ...],
    "piiData": {
        "key1": ["v1", "v2", ...],
        "key2": ["v1", "v2", ...],
        ...
    }
}
"""
@app.route('/gen-matching-key', methods=['POST'])
def generate_matching_key():
    data: Dict[str, Any] = request.get_json()
    if (
        not data or
        'columns' not in data or 
        'piiData' not in data or 
        not isinstance(data['piiData'], dict)
    ):
        return jsonify({'error': '올바르지 않은 요청입니다. columns와 pii-data를 포함해야 합니다.'}), 400
    
    # JSON 데이터에서 DataFrame 생성
    try:
        df = pd.DataFrame(data['piiData'])

    except ValueError as e:
        return jsonify({'error': '올바르지 않은 JSON 형식입니다.'}), 400
    
    def hash_row(row):
        concatenated = "".join(row.astype(str))
        return hashlib.sha256(concatenated.encode('utf-8')).hexdigest()

    df['matching_key'] = df.apply(hash_row, axis=1)
    print('[debug] df with matching_key:', df)

    return "", 200
    

if __name__ == '__main__':
    port = 1783
    register_url = 'http://localhost:1780/register'
    callback_url = f'http://localhost:{port}/gen-matching-key'

    ok = subscribe(
        register_url=register_url,
        topic='pii detection success',
        callback_url=callback_url,
        require_pii_data=True,
        count=3,
        interval=5
    )
    if not ok:
        exit(1)

    # app.run(port=port, debug=True)
    app.run(port=port)