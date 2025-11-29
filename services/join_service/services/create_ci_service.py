
from functools import reduce
import hashlib
from pathlib import Path
import re
from typing import Dict, List

import pandas as pd

from py_client.agent import Agent
from py_client.message.message import Message
from services.find_candidate_column_service import find_candidate_columns
from utils import CI_FILE_NAME, PROJECT_DIR, ProjectStatus, add_project_option, get_project_information

CREATE_CI = 4

def create_ci_service(agent: Agent, topic_name: str, message: Message):
    try:
        if (project_id := message.get_header("project.id")) is None:
            raise ValueError("헤더에 project_id가 없음")
        
        if (project_information := get_project_information(project_id)) is None:
            raise ValueError("프로젝트 정보가 없음")
        
        file_paths = [PROJECT_DIR / project_id / file_path for  file_path in project_information.get("files", [])]
        cardinality_threshold = float(value) if (value := message.get_header("cardinality.threshold")) else None
        select_threshold = float(value) if (value := message.get_header("select.threshold")) else None

        create_ci_file(
            project_id=project_id, file_paths=file_paths, 
            cardinality_threshold=cardinality_threshold, select_threshold=select_threshold
        )
        
    except Exception as e:
        print(f"? craete_ci_service(): {e}")
        
        message.add_header("error", str(e))

    agent.producer.asyncProduce(topic_name=topic_name, partition=str(-CREATE_CI), header=message.header)
    print("! create_ci_service(): 작업 완료")

# 결합 연계정보 생성을 위한 일련의 작업을 수행
def create_ci_file(project_id: str, file_paths: List[Path], cardinality_threshold: float | None = None, select_threshold: float | None = None):
    dataframes: Dict[str, pd.DataFrame] = {}
    for file_path in file_paths:
        if not file_path.exists():
            continue
        
        dataframes[file_path.name] = pd.read_csv(file_path)

    candidate_columns = find_candidate_columns(dataframes, cardinality_threshold, select_threshold)

    cis: Dict[str, pd.DataFrame] = {}
    for file_name, columns in candidate_columns.items():
        cis[file_name] = create_ci(file_name, dataframes[file_name], columns)

    merged = merge(cis)
    print(f"! create_ci_file(): 결합 연계정보 생성 완료: {merged.head(5)}")

    merged.to_csv(PROJECT_DIR / project_id / CI_FILE_NAME, index=False)
    for file_name, dataframe in dataframes.items():
        dataframe.to_csv(PROJECT_DIR / project_id / file_name, index=False)

    # 프로젝트 상태 업데이트
    add_project_option(project_id=project_id, key="ci", value=True)
    add_project_option(project_id=project_id, key="status", value=ProjectStatus.ACTIVE)

# 결합 연계정보 생성
def create_ci(file_name: str, dataframe: pd.DataFrame, candidate_columns: List[str]):
    def standardize(string: str):
        return re.sub(r"[-_*\s]", "", string)
    
    dataframe["join_serial_number"] = f"{file_name}." + dataframe.index.astype(str)

    if "join_serial_number" in candidate_columns:
        candidate_columns.remove("join_serial_number")

    ci = dataframe.assign(
        join_key=lambda df: df[candidate_columns].astype(str) \
            .apply(lambda row: row.map(standardize)) \
            .apply(lambda row: "".join(sorted(row)), axis=1) \
            .apply(lambda x: hashlib.sha256(x.encode("utf-8")).hexdigest())
    )[["join_key", "join_serial_number"]]

    return ci    

def merge(cis: Dict[str, pd.DataFrame]) -> pd.DataFrame:
    dfs = []
    for file_name, dataframe in cis.items():
        tmp = dataframe.copy()
        tmp = tmp.rename(columns={"join_serial_number": f"{file_name}_join_serial_number"})
        dfs.append(tmp)

    merged = reduce(lambda left, right: pd.merge(left, right, on="join_key", how="inner"), dfs) \
        .drop(columns=["join_key"]) \

    return merged

if __name__ == "__main__":
    project_id = "9ae1f6e2-99cc-44b6-b792-48c2f595fc63"
    create_ci_file(
        project_id=project_id,
        file_paths=[
            PROJECT_DIR / project_id / "data1.csv",
            PROJECT_DIR / project_id / "data2.csv",
            PROJECT_DIR / project_id / "data3.csv"
        ]
    )