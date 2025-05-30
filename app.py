from flask import Flask
from api_gateway_utils import subscribe
from controller.detection_controller import detection_bp

app = Flask(__name__)
app.register_blueprint(detection_bp)

@app.route('/')
def home():
    return "PII(Personally Identifiable Information) Detection API", 200

    
if __name__ == '__main__':
    port = 1782
    callback_url = f'http://localhost:{port}/pii-detection'
    
    subscribe(topic='pii.detection.request', callback_url=callback_url, count=3, interval=5)
    
    # app.run(port=port, debug=True)
    app.run(port=port)