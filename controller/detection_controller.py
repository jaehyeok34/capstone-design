from flask import Blueprint, jsonify, request
from dto.pii_detection_response import PiiDetectionResponse
from service.detection_service import detect


detection_bp = Blueprint('detection', __name__)

@detection_bp.route('/pii-detections', methods=['POST'])
def pii_detections() -> PiiDetectionResponse:
    dataset_info_list = request.get_json()
    if (not dataset_info_list) or (not isinstance(dataset_info_list, list)):
        return "dataset info 배열이 필요합니다.", 400
    
    try:
        response = detect(dataset_info_list)
        print(f'[debug] {response}')
        return jsonify(response.model_dump()), 200
    
    except Exception as e:
        print(f'[debug] {e}')
        return f"식별정보 탐지 실패: {e}", 500


