from flask import Blueprint, jsonify, request
from service.csv_service import get_all_values, get_column_values, get_columns, save_file

csv_controller = Blueprint('csv_controller', __name__, url_prefix='/csv')

@csv_controller.route('/upload', methods=['POST'])
def upload():
    try:
        if 'file' not in request.files:
            raise Exception('파일이 없음')
        
        file = request.files['file']
        if file.filename == '':
            raise Exception('파일 이름이 없음')
            
        name = save_file(file)
        return name, 200
    except Exception as e:
        return f'{request.path}: {e}', 400
    

@csv_controller.route('/columns/<string:dataset_info>', methods=['GET'])
def columns(dataset_info: str):
    try:
        columns = get_columns(dataset_info)
        
        return jsonify(columns), 200
    except Exception as e:
        return f'{request.path}: {e}', 400
    

@csv_controller.route('/column-values/<string:dataset_info>', methods=['POST'])
def column_values(dataset_info: str):
    try:
        data = request.get_json()
        if (not data) or (not isinstance(data, list)):
            raise Exception('요청 데이터가 없거나 잘못된 형식')

        filtered = get_column_values(dataset_info, data)
        return jsonify(filtered), 200
    except Exception as e:
        return f'{request.path}: {e}', 400
    

@csv_controller.route('/all-values/<string:dataset_info>', methods=['GET'])
def all_values(dataset_info: str):
    try:
        all_data = get_all_values(dataset_info)
        return jsonify(all_data), 200        
    except Exception as e:
        return f'{request.path}: {e}', 400