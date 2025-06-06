import os
from flask import Flask, g
import pymysql
from api_gateway_utils import subscribe_topic
from controller.detection_controller import detection_bp
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)
app.config['DB_CONFIG'] = {
    "host": os.getenv('DB_HOST', 'localhost'),
    "port": int(os.getenv('DB_PORT', 3306)),
    "user": os.getenv('DB_USER', 'root'),
    "password": os.getenv('DB_PASSWORD', '0000'),
    "database": os.getenv('DB_NAME', 'term_db'),
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor
}
app.config['SBERT_MODEL_PATH'] = os.getenv('SBERT_MODEL_PATH', 'model/sbert_domain_model/')
app.config['THRESHOLD'] = os.getenv('THRESHOLD', 0.84)
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
    host = os.getenv('HOST', '0.0.0.0')
    # port = os.getenv('PORT', 1782)
    port = os.getenv('PORT', 1234)
    container_name = os.getenv('CONT_NAME', 'pii-detection-server')
    callback_url = f'http://{container_name if host == '0.0.0.0' else host}:{port}/pii-detection/detect'

    # subscribe_topic(
    #     name='pii.detection.request', 
    #     callback_url=callback_url, 
    #     method='GET',
    #     use_path_variable=True,

    #     count=3, 
    #     interval=5
    # )
    
    app.run(host=host, port=port)