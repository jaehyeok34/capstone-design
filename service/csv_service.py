import os
from typing import Dict, List
import pandas as pd
from werkzeug.datastructures import FileStorage
from werkzeug.utils import secure_filename
from datetime import datetime
from flask import current_app

def save_file(file: 'FileStorage') -> str:
    try:
        dir = current_app.config['DATA_DIR']
        name, ext = os.path.splitext(secure_filename(file.filename))
        timestamp = datetime.now().strftime('%Y%m%d%H%M%S%f')
        unique_name = f"{name}_{timestamp}{ext}"

        os.makedirs(dir, exist_ok=True)
        file_path = os.path.join(dir, unique_name)
        file.save(file_path)

        return unique_name
    except Exception as _:
        raise Exception(f"save_uploaded_file(): 파일 저장 실패({file.filename})")


def get_columns(datasetInfo: str) -> List[str]:
    dir = current_app.config['DATA_DIR']
    file_path = os.path.join(dir, datasetInfo)
    if not os.path.exists(file_path):
        raise Exception(f'{datasetInfo} 파일이 없음')
        
    df = pd.read_csv(file_path)
    return df.columns.tolist()


def get_column_values(datasetInfo: str, columns: List[str]) -> Dict:
        dir = current_app.config['DATA_DIR']
        file_path = os.path.join(dir, datasetInfo)
        if not os.path.exists(file_path):
            raise Exception(f'{datasetInfo} 파일이 없음')

        df = pd.read_csv(file_path)
        exist_columns = [column for column in columns if column in df.columns]
        if not exist_columns:
            raise Exception(f'해당하는 컬럼이 없음({columns})')
        
        return df[exist_columns].to_dict()