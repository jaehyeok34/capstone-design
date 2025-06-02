from flask import Blueprint, request, jsonify

from service.pseudonymization_service import pseudonymization

pseudonymization_bp = Blueprint('pseudonymization', __name__, url_prefix='/pseudonymization')

@pseudonymization_bp.route('/pseudonymize/<string:dataset_info>', methods=['GET'])
def pseudonymize(dataset_info: str):
    try:
        pseudonymization(dataset_info)
        return '', 200

    except Exception as e:
        print(f'[debug] 실패: {e}')
        return f'{request.path} 실패: {e}', 500