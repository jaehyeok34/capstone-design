from flask import Blueprint, jsonify, request
from service.detection_service import detect


detection_bp = Blueprint('detection', __name__)

@detection_bp.route('/pii-detections', methods=['POST'])
def pii_detections():
    dataset_info_list = request.json()
    if (not dataset_info_list) or (not isinstance(dataset_info_list, list)):
        return "dataset info 배열이 필요합니다.", 400
    
    try:
        detect(dataset_info_list)
    except Exception as e:
        return f"식별정보 탐지 실패: {e}", 500

    return "식별정보 탐지 성공", 200


            
        # dataset_info = request.json.get('dataset_info')
        # if not dataset_info:
        #     return jsonify({"error": "dataset_info is required"}), 400
        
        # pii = detect(dataset_info)
        # pii = [x[0] for x in pii]
        # return jsonify(pii), 200

