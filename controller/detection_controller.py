from flask import Blueprint, jsonify, request
from service.detection_service import detect


detection_bp = Blueprint('detection', __name__, url_prefix='/pii-detection')

@detection_bp.route('/detect/<string:dataset_info>', methods=['GET'])
def pii_detection(dataset_info: str):
    try:
        pii, non_pii = detect(dataset_info)
        return jsonify({'pii': pii, 'non_pii': non_pii}), 200

    except Exception as e:
        return f'{request.path}: {e}', 500

