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
    port = 1784

    subscribe_topic(
        name='pseudonymization.pseudonymize.request',
        callback_url=f'http://localhost:{port}/pseudonymization/pseudonymize',
        method='GET',
        use_path_variable=True,

        count=3,
        interval=5
    )
    
    app.run(port=port)