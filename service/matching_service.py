import os
from typing import Dict, List
from uuid import UUID
from flask import current_app
import pandas as pd
from api_gateway_utils import get_all_values, publish_event, register_csv
from functools import reduce


def match(dataset_info_list: List[str], result_info: str):
    try:
        # 데이터 서버에서 데이터셋 획득
        df_dict = __get_dataset(dataset_info_list)

        # 일련번호와 결합키 컬럼 추출
        __extract_columns(df_dict)
        
        # 데이터셋 병합
        merged_df = __merge(df_dict)
        
        # 저장 및 업로드
        file_name = f'matched_{result_info}.csv'
        save_path = os.path.join(current_app.config['DATA_DIR'], file_name)
        merged_df.to_csv(save_path, index=False)
        register_csv(save_path)

        # 이벤트 발행
        publish_event(
            name='matching.match.success',
            path_variable=result_info
        )

        return merged_df

    except Exception as e:
        raise Exception(f'match() 실패: {e}')
    

def __get_dataset(dataset_info_list: List[str]) -> Dict:
    try:
        df_dict: Dict = {}
        for dataset_info in dataset_info_list:
            df_dict[dataset_info] = get_all_values(dataset_info)

        return df_dict
    except Exception as e:
        raise Exception(f'get_dataset() 실패: {e}')
    

def __extract_columns(df_dict: Dict):
    try:
        extract_columns = ['mk_serial_number', 'matching_key']
        for key, df in df_dict.items():
            # key: dataset_info, value: DataFrame
            columns = df.columns.tolist()
            for col in extract_columns:
                if col not in columns:
                    raise Exception(f'{key}에는 {col} 컬럼이 없습니다.')
                
            df_dict[key] = df[extract_columns]

    except Exception as e:
        raise Exception(f'extract_columns() 실패: {e}')
    

def __merge(df_dict: Dict) -> pd.DataFrame:
    try:
        renamed_dfs = []
        for key, df in df_dict.items():
            renamed_df = df.rename(columns={'mk_serial_number': key})
            renamed_dfs.append(renamed_df)

        # matching_key를 기준으로 순차적으로 병합
        merged_df = reduce(lambda left, right: pd.merge(left, right, on='matching_key', how='inner'), renamed_dfs)
        merged_df = merged_df.drop(columns=['matching_key'])

        return merged_df
    
    except Exception as e:
        raise Exception(f'merge() 실패: {e}')