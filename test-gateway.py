from flask import Flask, jsonify, request
from typing import Dict

app = Flask(__name__)

@app.route('/', methods=['POST'])
def home():
    data: Dict[str, str] = request.get_json()
    if (not data) or ('topic' not in data) or ('url' not in data):
        return jsonify(
            {'error': '올바르지 않은 요청입니다. "topic"과 "url" 필드가 존재해야 합니다.'}
        ), 400

    print("topic:", data.get('topic'))
    print("url:", data.get('url'))

    return "", 200

if __name__ == '__main__':
    app.run(port=1780, debug=True)