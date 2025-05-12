import random
from flask import Flask
from flask import request

app = Flask(__name__)

@app.route('/')
def home():
    return "Hello, World!"

@app.route('/detect', methods=['POST'])
def detect():
    data = request.get_json()
    if not isinstance(data, list):
        return {"error": "Invalid data format, expected a list"}, 400

    # TODO: 도메인 사전/임베딩 모델을 통해 식별정보 탐지하는 로직을 구현해서 넣어야함
    # 현재는 테스트로 body로 들어온 컬럼명을 랜덤한 개수의 랜덤한 값을 식별자로 반환하도록 구현함
    result = random.sample(data, k=random.randint(1, len(data)))
    return {"identity": result}, 200
    
if __name__ == '__main__':
    app.run(port=1782, debug=True)