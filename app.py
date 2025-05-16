from flask import Flask
import pandas as pd
from flask import jsonify

app = Flask(__name__)

@app.route('/')
def home():
    return "data-server"


@app.route('/get-pii-data', methods=['GET'])
def get_data():
    try:
        df = pd.read_csv('datas.csv')
        return jsonify(df.to_dict(orient='dict')), 200
    
    except Exception as e:
        print(f"[debug]: {e}")
        return "", 500


if __name__ == '__main__':
    port = 1789

    # app.run(port=port, debug=True)
    app.run(port=port)