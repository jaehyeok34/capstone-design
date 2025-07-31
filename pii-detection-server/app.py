from flask import Flask, g
import pymysql
from api_gateway_utils import subscribe_topic
from config.env import Env
from controller.detection_controller import detection_bp

env = Env()
app = Flask(__name__)
app.config['DB_CONFIG'] = {
    "host": env.db_host,
    "port": env.db_port,
    "user": env.db_user,
    "password": env.db_password,
    "database": env.db_name,
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor
}
app.config['SBERT_MODEL_PATH'] = env.sbert_model_path
app.config['THRESHOLD'] = env.threshold
app.register_blueprint(detection_bp)


@app.route('/')
def home():
    return "PII(Personally Identifiable Information) Detection API", 200


@app.teardown_appcontext
def close_db(exception):
    db = g.pop('db', None)
    if db is not None:
        db.close()

    
if __name__ == '__main__':
    host = env.host
    port = env.port
    service_name = env.service_name
    callback_url = f'http://{service_name}:{port}/pii-detections'

    subscribe_topic(
        name='pii.detection.request', 
        callback_url=callback_url, 
        method='POST',
        use_path_variable=False,

        count=3, 
        interval=5
    )
    
    app.run(host=host, port=port)