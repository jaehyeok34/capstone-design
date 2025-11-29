
from pathlib import Path
from typing import Dict, List
import pandas as pd
from py_client.agent import Agent
from py_client.message.message import Message
from utils import CI_FILE_NAME, JOINED_FILE_NAME, PROJECT_DIR, PSEUDONYMIZED_FILE_NAME, ProjectStatus, add_project_option, get_project_information
from modules.pseudonymize import pseudonymize_dataframe

JOIN = 5
GET_PSEUDONYMIZED_FILE = 7

def join_service(agent: Agent, topic_name: str, message: Message):
    try:
        if not (project_id := message.get_header("project.id")):
            raise ValueError("헤더에 project_id가 없음")
        
        if (joined := join(project_id)) is None:
            raise ValueError("데이터 결합 실패")
        
        pseudonymized = pseudonymize_dataframe(df=joined)
        with open(PROJECT_DIR / project_id / PSEUDONYMIZED_FILE_NAME, "w", encoding="utf-8") as f:
            pseudonymized.to_csv(f, index=False)

        add_project_option(project_id=project_id, key="status", value=ProjectStatus.DONE)
        
    except Exception as e:
        print(f"? join_service(): {e}")
        message.add_header("error", str(e))

    agent.producer.asyncProduce(topic_name=topic_name, partition=str(-JOIN), header=message.header)

    print(f"! join_service(): 데이터 결합 완료")

def get_pseudonymized_file_service(agent: Agent, topic_name: str, message: Message):
    file_content = None
    try:
        if not (project_id := message.get_header("project.id")):
            raise ValueError("헤더에 project_id가 없음")
        
        if not (file_content := get_pseudonymized_file(project_id)):
            raise ValueError("결합된 파일이 없음")

    except Exception as e:
        print(f"? get_pseudonymized_file_service(): {e}")
        message.add_header("error", str(e))

    agent.producer.asyncProduce(topic_name=topic_name, partition=str(-GET_PSEUDONYMIZED_FILE), header=message.header, payload=file_content)

    print(f"! get_pseudonymized_file_service(): 완료")

def join(project_id: str) -> pd.DataFrame | None:
    if not (project_information := get_project_information(project_id)):
        return None
    
    if not (project_information.get("ci", False)):
        return None
    
    ci_path = PROJECT_DIR / project_id / CI_FILE_NAME
    if not ci_path.exists():
        return None
    
    sources = {
        file_name: pd.read_csv(PROJECT_DIR / project_id / file_name)
        for file in project_information.get("files", [])
        if (file_name := Path(file).name)
    }

    ci = pd.read_csv(ci_path)
    joined = ci.copy()
    for file_name, dataframe in sources.items():
        key = f"{file_name}_join_serial_number"
        df_indexed = dataframe.add_prefix(f"{file_name}_") \
            .set_index(f"{file_name}_join_serial_number")

        joined = joined.merge(df_indexed, left_on=key, right_index=True)      

    candidate_columns: Dict[str, List[str]] = project_information.get("candidateColumns", {})

    # 서로다른 파일에 존재하는 결합키 생성에 사용된 컬럼을 하나만 남기고 모두 제거하기 위해
    candidate_columns.pop(next(iter(candidate_columns)), None) 

    joined = joined.drop(columns=list(ci.columns))
    for file_name, columns in candidate_columns.items():
        prefixed_columns = [f"{file_name}_{col}" for col in columns]
        joined = joined.drop(columns=prefixed_columns, errors='ignore')
        
    with open(PROJECT_DIR / project_id / JOINED_FILE_NAME, "w", encoding="utf-8") as f:
        joined.to_csv(f, index=False)

    return joined

def get_pseudonymized_file(project_id: str) -> bytes | None:
    pseudonymized_path = PROJECT_DIR / project_id / PSEUDONYMIZED_FILE_NAME
    if not pseudonymized_path.exists():
        return None

    with open(pseudonymized_path, "rb") as f:
        return f.read()

def get_joined_file(project_id: str) -> bytes | None:
    joined_path = PROJECT_DIR / project_id / JOINED_FILE_NAME
    if not joined_path.exists():
        return None

    with open(joined_path, "rb") as f:
        return f.read()