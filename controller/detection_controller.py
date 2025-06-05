from flask import Blueprint, jsonify, request
from typing import List
from service.detection_service import detect


detection_bp = Blueprint('detection', __name__, url_prefix='/pii-detection')

@detection_bp.route('/detect/<string:dataset_info>', methods=['GET'])
def pii_detection(dataset_info: str):
    try:
        detected: List[str] = detect(dataset_info)
        return jsonify(detected), 200

    except Exception as e:
        return f'{request.path}: {e}', 500

