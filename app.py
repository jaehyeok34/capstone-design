import random
from flask import Flask
from flask import request, jsonify
from typing import Dict, List
import requests
from register import Register

app = Flask(__name__)

@app.route('/')
def home():
    return "PII(Personally Identifiable Information) Detection API", 200


@app.route('/detect', methods=['POST'])
def detect():
    data: Dict[str, List[str]] = request.get_json()
    if (
        not data or                             # 데이터가 비어 있거나
        'columns' not in data or                # 'columns' 필드가 없거나
        not isinstance(data['columns'], list)   # 'columns' 필드가 리스트 형식이 아닐 경우
    ):
        return jsonify(
            {'error': '올바르지 않은 요청입니다. "columns" 필드가 존재해야 하며, 리스트 형식이어야 합니다.'}
        ), 400

    # TODO: 도메인 사전/임베딩 모델을 통해 식별정보 탐지하는 로직을 구현해서 넣어야함
    # 현재는 테스트로 body로 들어온 컬럼명을 랜덤한 개수의 랜덤한 값을 식별자 판단하고 반환하도록 구현함
    columns = data['columns']
    result = random.sample(columns, k=random.randint(1, len(columns)))
    print(f"[debug] result: {result}")

    # API Gateway의 /event로 결과 전송 해야함(post)

    return "", 200

    
if __name__ == '__main__':
    register_url = 'http://127.0.0.1:1780/register'
    callback_url = 'http://127.0.1:1782/detect'

    # for topic in ['input', 'low_match_rate']:
    for topic in ['input']:
        ok = Register.subscribe(
            register_url=register_url, 
            topic=topic, 
            callback_url=callback_url, 
            count=3, 
            interval=5
        )
        if not ok:
            exit(1)

    # 토픽 구독 등록 이후 서버 구동
    app.run(port=1782, debug=True)