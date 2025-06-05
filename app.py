import os
from flask import Flask
from api_gateway_utils import subscribe_topic
from controller.detection_controller import detection_bp

app = Flask(__name__)
app.register_blueprint(detection_bp)

@app.route('/')
def home():
    return "PII(Personally Identifiable Information) Detection API", 200

    
if __name__ == '__main__':
    host = os.getenv('HOST', '0.0.0.0')
    port = os.getenv('PORT', 1782)
    container_name = os.getenv('CONT_NAME', 'pii-detection-server')
    callback_url = f'http://{container_name if host == '0.0.0.0' else host}:{port}/pii-detection/detect'

    subscribe_topic(
        name='pii.detection.request', 
        callback_url=callback_url, 
        method='GET',
        use_path_variable=True,

        count=3, 
        interval=5
    )
    
    app.run(host=host, port=port)