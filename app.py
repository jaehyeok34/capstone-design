from flask import Flask
import os
from controller.csv_conroller import csv_controller


app = Flask(__name__)
app.config['DATA_DIR'] = os.path.join(os.path.dirname(__file__), 'data')
os.makedirs(app.config['DATA_DIR'], exist_ok=True)
app.register_blueprint(csv_controller)


@app.route('/')
def home():
    return "data-server"

if __name__ == '__main__':
    port = 1789

    # app.run(port=port, debug=True)
    app.run(port=port)