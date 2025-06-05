import os
from flask import Flask
from api_gateway_utils import subscribe_topic
from controller.pseudonymization_controller import pseudonymization_bp

app = Flask(__name__)
app.config['DATA_DIR'] = os.path.join(os.path.dirname(__file__), 'pseudonymizaed')
os.makedirs(app.config['DATA_DIR'], exist_ok=True)
app.register_blueprint(pseudonymization_bp)

@app.route('/')
def home():
    return 'Pseudonymization Service', 200

if __name__ == '__main__':
    # port = 1784
    host = os.getenv('HOST', '0.0.0.0')
    port = os.getenv('PORT', 1784)
    container_name = os.getenv('CONT_NAME', 'pseudonymization-server')
    callback_url = f'http://{container_name if host == "0.0.0.0" else host}:{port}/pseudonymization/pseudonymize'

    subscribe_topic(
        name='pseudonymization.pseudonymize.request',
        callback_url=callback_url,
        method='GET',
        use_path_variable=True,

        count=3,
        interval=5
    )
    
    app.run(host=host, port=port)