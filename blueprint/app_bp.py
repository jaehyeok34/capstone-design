from flask import Blueprint
from service.app_service import AppService

app_bp = Blueprint('app_bp', __name__)
app_service = AppService()

@app_bp.route('/', methods=['GET'])
def home():
    return app_service.say_hello()