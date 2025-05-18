from dataclasses import dataclass
import random
from flask import Flask
from flask import request
from typing import List
from api_gateway import subscribe, request_post, request_get_columns, topic_subscribe_url

@dataclass
class PIIDetectionDTO:
    selectedRegisteredData: List[str]


app = Flask(__name__)

@app.route('/')
def home():
    return "PII(Personally Identifiable Information) Detection API", 200


@app.route('/detect', methods=['POST'])
def detect():
    try:
        data = PIIDetectionDTO(**request.get_json())
    except TypeError as _:
        return "[debug] selectRegisteredData가 있어야 함", 400
    
    # columns 가져오기
    columns = {}
    for selected_title in data.selectedRegisteredData:
        print(f"[debug] selected_title: {selected_title}")
        result = request_get_columns(selected_title)
        if result is None:
            return "[debug] get_columns 실패", 400
        
        columns[selected_title] = result

    # TODO: 도메인 사전/임베딩 모델을 통해 식별정보 탐지하는 로직을 구현해서 넣어야함
    # 현재는 테스트로 body로 들어온 컬럼명을 랜덤한 개수의 랜덤한 값을 식별자 판단하고 반환하도록 구현함
    identifiers = {}
    for title, columns in columns.items():
        result = random.sample(columns, k=random.randint(1, len(columns)))
        identifiers[title] = result

    print(f"[debug] identifiers: {identifiers}")

    # API Gateway의 /event로 결과 전송 해야함(post)
    request_post(
        target_url='http://127.0.0.1:1780/event',
        event='matching-key.request',
        data={'pii-columns': identifiers}
    )

    return "", 200

    
if __name__ == '__main__':
    port = 1782
    callback_url = f'http://127.0.0.1:{port}/detect'

    for topic in ['pii.detection.request']:
        ok = subscribe(
            topic=topic, 
            callback_url=callback_url, 
            count=3, 
            interval=5
        )
        if not ok:
            exit(1)

    # 토픽 구독 등록 이후 서버 구동
    # app.run(port=port, debug=True)
    app.run(port=port)