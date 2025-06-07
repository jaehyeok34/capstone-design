import os
from flask import Flask
from api_gateway_utils import subscribe_topic
from config.env import Env
from controller.matching_key_controller import matching_key_bp


env = Env()
app = Flask(__name__)
app.register_blueprint(matching_key_bp)


@app.route('/')
def home():
    return "matching-key-server"
    

if __name__ == '__main__':
    callback_url = f'http://{env.service_name}:{env.port}/matching-keys'

    for topic_name in ['matching-key.generate.request', 'pii.detection.success']:
        subscribe_topic(
            name=topic_name,
            callback_url=callback_url,
            method='POST',
            use_path_variable=False,

            count=3,
            interval=5
        )

    app.run(host=env.host, port=env.port)