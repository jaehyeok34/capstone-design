import os
from flask import Flask
from api_gateway_utils import subscribe_topic
from controller.matching_key_controller import matching_key_bp


app = Flask(__name__)
app.config['DATA_DIR'] = os.path.join(os.path.dirname(__file__), 'generated_mk')
os.makedirs(app.config['DATA_DIR'], exist_ok=True)
app.register_blueprint(matching_key_bp)

@app.route('/')
def home():
    return "matching-key-server"
    

if __name__ == '__main__':
    port = 1783
    callback_url = f'http://localhost:{port}/matching-key/generate'

    for topic_name in ['matching-key.generate.request', 'pii.detection.success']:
        subscribe_topic(
            name=topic_name,
            callback_url=callback_url,
            method='POST',
            use_path_variable=True,

            count=3,
            interval=5
        )

    # app.run(port=port, debug=True)
    app.run(port=port, host='0.0.0.0')