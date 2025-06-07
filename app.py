import flask
from api_gateway_utils import subscribe_topic
from config.env import Env
from controller.matching_controller import matching_bp

env = Env()
app = flask.Flask(__name__)
app.register_blueprint(matching_bp)


@app.route('/')
def home():
    return "matching server", 200


if __name__ == '__main__':
    
    callback_url = f'http://{env.service_name}:{env.port}/dataset-matchings'
    subscribe_topic(
        name='matching-key.generate.success',
        callback_url=callback_url,
        method='POST',
        use_path_variable=False,

        count=3,
        interval=5
    )

    app.run(host=env.host, port=env.port)