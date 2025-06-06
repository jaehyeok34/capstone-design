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
        dataset_info = f'{name}_{timestamp}'
        file_path = os.path.join(dir, f'{dataset_info}{ext}')

        file.save(file_path)

        return dataset_info
    
    except Exception as _:
        raise Exception(f"save_uploaded_file(): 파일 저장 실패({file.filename})")


def get_columns(dataset_info: str) -> List[str]:
    dir = current_app.config['DATA_DIR']
    file_path = os.path.join(dir, dataset_info + ".csv")
    if not os.path.exists(file_path):
        raise Exception(f'{dataset_info} 파일이 없음')
        
    df = pd.read_csv(file_path)
    return df.columns.tolist()


def get_column_values(dataset_info: str, columns: List[str]) -> Dict:
    try:
        file_path = __get_file(dataset_info)
        df = pd.read_csv(file_path)
        exist_columns = [column for column in columns if column in df.columns]
        if not exist_columns:
            raise Exception(f'해당하는 컬럼이 없음({columns})')
        
        return df[exist_columns].to_dict()
    except Exception as e:
        raise Exception(f'get_column_values() 실패: {e}')


def get_all_values(dataset_info: str) -> Dict:
    try:
        file_path = __get_file(dataset_info)
        df = pd.read_csv(file_path)

        return df.to_dict()
    except Exception as e:
        raise Exception(f'get_all_values() 실패: {e}')
    

def get_cardinality(dataset_info: str, column: str) -> Dict:
    try:
        file_path = __get_file(dataset_info)
        df = pd.read_csv(file_path)
        
        if column not in df.columns:
            raise Exception(f'해당하는 컬럼이 없음({column})')
        
        cardinality = int(df[column].nunique())
        record = int(df[column].count())

        return {'cardinality': cardinality, 'record': record}
    except Exception as e:
        raise Exception(f'get_cardinality() 실패: {e}')


def __get_file(dataset_info: str) -> str:
    dir = current_app.config['DATA_DIR']
    file_path = os.path.join(dir, dataset_info + ".csv")
    if not os.path.exists(file_path):
        raise Exception(f'{dataset_info} 파일이 없음')
    
    return file_path