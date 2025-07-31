from flask import Blueprint, request
from dto.matching_key_reqeust import MatchingKeyRequest
from service.matching_key_service import generate_matching_key
from pydantic import ValidationError


matching_key_bp = Blueprint('matching-key', __name__)


@matching_key_bp.route('/matching-keys', methods=['POST'])
def matching_keys():
    data = request.get_json()
    if not data:
        return "MatchingKeyRequest가 필요합니다.", 400
    
    try:
        matching_key_request = MatchingKeyRequest(**data)
        generate_matching_key(matching_key_request)

        return "", 200
    
    except ValidationError as e:
        return f'잘못된 형식입니다. MatchingKeyRequest 형식이 필요합니다: {e}', 400
    
    except Exception as e:
        return f'결합키 생성 과정에서 문제 발생: {e}', 500

    

