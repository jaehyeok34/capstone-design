from flask import Blueprint, jsonify, request

from service.matching_key_service import generate_matching_key

matching_key_bp = Blueprint('matching_key', __name__, url_prefix='/matching-key')

@matching_key_bp.route('/generate/<string:dataset_info>', methods=['POST'])
def gen_matching_key(dataset_info: str):
    try:
        data = request.get_json()
        if (not data) or (not isinstance(data, list)):
            raise Exception("잘못된 body 형식입니다. JSON Array가 필요합니다.")
        
        result = generate_matching_key(dataset_info, data)
        return jsonify(result.to_dict()), 200

    except Exception as e:
        return f'{request.path}: {e}', 400
    
