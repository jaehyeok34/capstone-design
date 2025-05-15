import random
from flask import Flask
from flask import request, jsonify
from typing import Dict, List

app = Flask(__name__)

@app.route('/')
def home():
    return "PII(Personally Identifiable Information) Detection API", 200


@app.route('/detect', methods=['POST'])
def detect():
    data: Dict[List[str]] = request.get_json()
    if (not data) or ('columns' not in data) or (not isinstance(data['columns'], list)):
        return jsonify(
            {'error': '올바르지 않은 요청입니다. "columns" 필드가 존재해야 하며, 리스트 형식이어야 합니다.'}
        ), 400  

    # TODO: 도메인 사전/임베딩 모델을 통해 식별정보 탐지하는 로직을 구현해서 넣어야함
    # 현재는 테스트로 body로 들어온 컬럼명을 랜덤한 개수의 랜덤한 값을 식별자로 반환하도록 구현함
    columns = data['columns']
    return jsonify(
        {'identity': random.sample(columns, k=random.randint(1, len(columns)))}
    ), 200

    
if __name__ == '__main__':
    app.run(port=1782, debug=True)