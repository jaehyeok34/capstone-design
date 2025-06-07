from flask import Flask
import os
from config.env import Env
from controller.csv_conroller import csv_bp

env = Env()
app = Flask(__name__)
app.config['DATA_DIR'] = os.path.join(os.path.dirname(__file__), 'data')
os.makedirs(app.config['DATA_DIR'], exist_ok=True)
app.register_blueprint(csv_bp)


@app.route('/')
def home():
    return "data-server"

if __name__ == '__main__':
    app.run(host=env.host, port=env.port)