from flask import Flask
import os
from controller.csv_conroller import csv_bp


app = Flask(__name__)
app.config['DATA_DIR'] = os.path.join(os.path.dirname(__file__), 'data')
os.makedirs(app.config['DATA_DIR'], exist_ok=True)
app.register_blueprint(csv_bp)


@app.route('/')
def home():
    return "data-server"

if __name__ == '__main__':
    host = os.getenv('HOST', '0.0.0.0')
    port = os.getenv('PORT', 1781)

    app.run(host=host, port=port)