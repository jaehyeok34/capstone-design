from flask import Flask
import pandas as pd
from flask import request, jsonify
import hashlib

app = Flask(__name__)

@app.route('/')
def home():
    return "matching-key-server"

@app.route('/gen-matching-key', methods=['POST'])
def generate_matching_key():
    try:
        data = request.get_json()
        if data is None:
            return jsonify({"error": "Invalid JSON"}), 400
        
        df = pd.DataFrame(data)

        # dataframe의 각 컬럼 값을 ""으로 join하고, 그 값을 sha256으로 hash한 값을 matching_key 컬럼으로 추가
        def hash_row(row):
            concatenated = "".join(row.astype(str))
            return hashlib.sha256(concatenated.encode('utf-8')).hexdigest()

        df['matching_key'] = df.apply(hash_row, axis=1)
        
        result = df.to_json()
        print(result)
        return result, 200
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(port=1783, debug=True)