from typing import List
from flask import Blueprint, request, jsonify

from service.matching_service import match

matching_bp = Blueprint('matching', __name__, url_prefix='/matching')

@matching_bp.route('/match/<string:result_info>', methods=['POST'])
def matching(result_info: str):
    try:
        data: List[str] = request.get_json()
        if (not data) or (not isinstance(data, list)):
            raise Exception('데이터가 없거나 잘못된 형식입니다.')

        match(data, result_info)
        return "", 200

    except Exception as e:
        print(f'[debug] matching() 실패: {e}')
        return f'{request.path} 실패: {e}', 500