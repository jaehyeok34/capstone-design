
import base64
import json
from typing import Dict, List

import pandas as pd

from py_client.agent import Agent
from py_client.message.message import Message
from utils import to_dataframe


CARDINALITY_THRESHOLD = 0.9
SELECT_THRESHOLD = 0.8
FIND_CANDIDATE_COLUMN = 1

def find_candidate_column_service(agent: Agent, topic_name: str, message: Message):
    try:
        json_files = json.loads(message.payload.decode("utf-8"))
        files = {file_name: base64.b64decode(content) for file_name, content in json_files.items()}

        dataframes: Dict[str, pd.DataFrame] = {}
        for file_name, file_content in files.items():
            dataframe = to_dataframe(file_content)
            if dataframe is None or dataframe.empty:
                continue

            dataframes[file_name] = dataframe
        
        if len(dataframes) < 2:
            raise ValueError("유효한 데이터프레임이 2개 미만임")
        
        candidate_columns = json.dumps(find_candidate_columns(dataframes))
    except Exception as e:
        print(f"? find_candidate_column_service.__handler(): {e}")
        
        message.add_header("error", str(e))            
        candidate_columns = None

    agent.producer.asyncProduce(
        topic_name=topic_name, partition=str(-FIND_CANDIDATE_COLUMN),
        header=message.header, payload=candidate_columns
    )
    
    print(f"! find_candidate_column_service(): 완료")

def find_candidate_columns(dataframes: Dict[str, pd.DataFrame], cardinality_threshold: float | None = None, select_threshold: float | None = None):
    unique_columns: Dict[str, List[str]] = {}

    for file_name, dataframe in dataframes.items():
        unique_columns[file_name] = select_unique_columns(dataframe, cardinality_threshold)

    candidate_columns: Dict[str, List[str]] = (
        {key: common_columns for key in dataframes.keys()}
        if (common_columns := select_common_columns(unique_columns))
        else select_candidate_columns(dataframes, unique_columns, select_threshold)
    )
    
    return candidate_columns

def select_unique_columns(dataframe: pd.DataFrame, threshold: float | None = None):
    if threshold is None:
        threshold = CARDINALITY_THRESHOLD

    total_rows = len(dataframe)
    ratios = dataframe.nunique(dropna=True) / total_rows

    return ratios[ratios >= threshold].index.tolist()

# 완전 일치하는 컬럼들 찾기
def select_common_columns(unique_columns: Dict[str, List[str]]) -> List[str]:
    return list(set.intersection(*(set(columns) for columns in unique_columns.values())))

# 기준 데이터셋의 컬럼을 순회하며 다른 데이터셋의 모든 컬럼과 비교하여 일치 비율이 임계치 이상인 컬럼들을 찾음
# 일치 비율이 가장 높은 컬럼이 임계치 이상이며, 모든 데이터셋에 존재한다면 매칭됐다고 판단하여 루프에서 제외시켜 성능 최적화
def select_candidate_columns(dataframes: Dict[str, pd.DataFrame], unique_columns: Dict[str, List[str]], select_threshold: float | None = None):
    if select_threshold is None:
        select_threshold = SELECT_THRESHOLD

    dataframes_keys = list(dataframes.keys())
    base_key = dataframes_keys[0]
    target_keys = dataframes_keys[1:]

    selected: Dict[str, Dict[str, str]] = {}
    for base_column in unique_columns[base_key]:
        for target_key in target_keys: # df1, df2, ...
            ratios: Dict[str, float] = {}
            for target_column in unique_columns[target_key]: # df1.col1, df1.col2, ...
                count = dataframes[base_key][base_column].isin(dataframes[target_key][target_column]).sum()
                ratios[target_column] = int(count) / min(len(dataframes[base_key]), len(dataframes[target_key]))

            max_key, max_ratio = max(ratios.items(), key=lambda x: x[1])
            if max_ratio >= select_threshold:
                selected.setdefault(base_column, {})[target_key] = max_key

        if len(selected.get(base_column, {})) == len(target_keys):
            for target_key, matched_column in selected[base_column].items():
                unique_columns[target_key].remove(matched_column)

        else:
            selected.pop(base_column, None)

    candidate_columns: Dict[str, List[str]] = {}
    for base_column, targets in selected.items():
        candidate_columns.setdefault(base_key, []).append(base_column)
        for target_key, selected_column in targets.items():
            candidate_columns.setdefault(target_key, []).append(selected_column)

    return candidate_columns