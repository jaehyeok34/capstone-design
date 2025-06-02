import math
import os
from flask import current_app
import pandas as pd
from api_gateway_utils import get_all_values, publish_event, register_csv


def pseudonymization(dataset_info: str):
    try:
        # 데이터 서버에서 dataset_info에 해당하는 데이터 가져오기
        df = get_all_values(dataset_info) 

        # 가명처리 수행하기
        for column, dtype in df.dtypes.items():
            if dtype == 'object':
                __pseudonymize_object(df, column)

            elif dtype == 'int64' or dtype == 'float64':
                __pseudonymize_numeric(df, column)

        # api gateway /csv/register를 통해 가명처리된 데이터 등록하기
        file_name = f'psudonymized_{dataset_info}.csv'
        save_path = os.path.join(current_app.config['DATA_DIR'], file_name)
        df.to_csv(save_path, index=False)
        register_csv(save_path)

        publish_event(
            name='pseudonymization.pseudonymize.success',
            path_variable=dataset_info
        )


    except Exception as e:
        raise Exception(f'pseudonymization() 실패: {e}')
    

def __pseudonymize_object(df: pd.DataFrame, column: str, rate: int = 0.7):
    '''
    컬럼 값이 문자열인 경우 마스킹처리
    '''
    def partial_masking(x):
            if isinstance(x, str):
                mask_len = math.ceil(int(len(x) * rate))
                return x[:len(x)-mask_len] + '*' * mask_len
            
            return x
    df[column] = df[column].apply(partial_masking)


def __pseudonymize_numeric(df: pd.DataFrame, column: str):
    '''
    컬럼 값이 숫자인 경우 범주화
    '''
    n = len(df[column])
    q = max(1, n // 2)
    bins = pd.qcut(df[column], q=q, duplicates='drop')
    df[column] = bins.apply(lambda x: f'{x.left}~{x.right}')