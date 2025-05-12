from flask import Flask
import pandas as pd
from flask import request, jsonify

app = Flask(__name__)

@app.route('/')
def home():
    return "data-server"

@app.route('/get-data', methods=['GET'])
def get_data():
    try:
        df = pd.read_csv('datas.csv')
        data = df.to_dict(orient='list')
        return jsonify(data)
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(port="1781", debug=True)