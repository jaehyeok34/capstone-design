import os
import tempfile
from typing import Dict, List
from flask import json
import pandas as pd
from api_gateway_utils import get_all_values, publish_event, register_csv
from functools import reduce


def match(dataset_info_list: List[str]):
    dataset_dict = __get_dataset(dataset_info_list)
    extracted_dataset = __extract_columns(dataset_dict)
    matched_df = __match(extracted_dataset)
    merged_df = __merge(list(dataset_dict.values()))
    
    with tempfile.TemporaryDirectory() as temp_dir:
        file_name = f'matched_{dataset_info_list[0]}.csv'
        path = os.path.join(temp_dir, file_name)
        matched_df.to_csv(path, index=False)
        new = register_csv(path)

        file_name = f"merged_{dataset_info_list[0]}.csv"
        path = os.path.join(temp_dir, file_name)
        merged_df.to_csv(path, index=False)
        register_csv(path)

    publish_event(
        name='matching.match.success',
        json_data=json.dumps(new)
    )
    

def __get_dataset(dataset_info_list: List[str]) -> Dict:
    df_dict: Dict = {}
    for dataset_info in dataset_info_list:
        df_dict[dataset_info] = get_all_values(dataset_info)

    return df_dict


def __extract_columns(df_dict: Dict) -> Dict:
    copied = df_dict.copy()
    extract_columns = ['mk_serial_number', 'matching_key']
    for key, df in copied.items():
        # key: dataset_info, value: DataFrame
        columns = df.columns.tolist()
        for col in extract_columns:
            if col not in columns:
                raise Exception(f'{key}에는 {col} 컬럼이 없습니다.')
            
        copied[key] = df[extract_columns]

    return copied

    
def __match(df_dict: Dict) -> pd.DataFrame:
    renamed_dfs = []
    for key, df in df_dict.items():
        renamed_df = df.rename(columns={'mk_serial_number': key})
        renamed_dfs.append(renamed_df)

    # matching_key를 기준으로 순차적으로 병합
    matched_df = reduce(lambda left, right: pd.merge(left, right, on='matching_key', how='inner'), renamed_dfs)
    matched_df = matched_df.drop(columns=['matching_key'])

    return matched_df


def __merge(dfs: List[pd.DataFrame]) -> pd.DataFrame:
    excepted = ['matching_key']
    common = set(dfs[0].columns)
    for df in dfs[1:]:
        common &= set(df.columns)

    for col in excepted:
        common.discard(col)

    cleaned = [dfs[0]] + [df.drop(columns=list(common), errors='ignore') for df in dfs[1:]]
    return reduce(lambda x, y: pd.merge(x, y, on='matching_key', how='inner'), cleaned)\
        .drop(columns=['matching_key', 'mk_serial_number'], errors='ignore')