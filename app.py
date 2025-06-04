import os
import flask
from api_gateway_utils import subscribe_topic
from controller.matching_controller import matching_bp

app = flask.Flask(__name__)
app.config['DATA_DIR'] = os.path.join(os.path.dirname(__file__), 'data')
os.makedirs(app.config['DATA_DIR'], exist_ok=True)
app.register_blueprint(matching_bp)

@app.route('/')
def home():
    return "matching server", 200


if __name__ == '__main__':
    port = 1785
    
    subscribe_topic(
        name='matching.match.request',
        callback_url=f'http://localhost:{port}/matching/match',
        method='POST',
        use_path_variable=True,

        count=3,
        interval=5
    )
    
    app.run(port=port)