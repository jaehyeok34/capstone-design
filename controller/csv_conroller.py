import os
from flask import Blueprint, current_app, jsonify, request
import pandas as pd
from service.csv_service import get_column_values, get_columns, save_file

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
    

@csv_controller.route('/columns/<string:datasetInfo>', methods=['GET'])
def columns(datasetInfo: str):
    try:
        if (not datasetInfo) or (datasetInfo == ''):
            raise Exception('datasetInfo가 없음')
        
        columns = get_columns(datasetInfo)
        
        return jsonify(columns), 200
    
    except Exception as e:
        return f'{request.path}: {e}', 400
    

@csv_controller.route('/column-values/<string:datasetInfo>', methods=['POST'])
def column_values(datasetInfo: str):
    try:
        if (not datasetInfo) or (datasetInfo == ''):
            raise Exception('datasetInfo가 없음')
        
        data = request.get_json()
        if (not data) or (not isinstance(data, list)):
            raise Exception('요청 데이터가 없음')

        filtered = get_column_values(datasetInfo, data)
        return jsonify(filtered), 200
    
    except Exception as e:
        return f'{request.path}: {e}', 400
    