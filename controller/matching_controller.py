from flask import Blueprint, request

from service.matching_service import match

matching_bp = Blueprint('matching', __name__)

@matching_bp.route('/dataset-matchings', methods=['POST'])
def dataset_machings():
    data = request.get_json()
    if (data is None) or (not isinstance(data, list)):
        return f'올바르지 않은 요청입니다.', 400
    
    match(data)
    return '', 200