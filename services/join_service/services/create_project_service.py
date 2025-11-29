import base64
from datetime import datetime
import json
from pathlib import Path
from typing import Dict, List
import uuid


from py_client.agent import Agent
from py_client.message.message import Message
from services.find_candidate_column_service import find_candidate_columns
from utils import PROJECT_DIR, ProjectStatus, save_project, to_dataframe

CREATE_PROJECT = 2

def create_project_service(agent: Agent, topic_name: str, message: Message):
    try:
        if not (payload := json.loads(message.payload.decode("utf-8"))):
            raise ValueError("유효하지 않은 페이로드")
        
        project_name = payload.get("projectName", "unknown")
        # processing_type = payload.get("processingType", "join")
        candidate_columns = payload.get("candidateColumns", {})
        files = {
            file_name: base64.b64decode(content)
            for file_name, content in payload.get("files", {}).items()
        }

        if not (project_id := create_project(project_name=project_name, candidate_columns=candidate_columns, files=files)):
            raise ValueError("프로젝트 생성 실패")
        
    except Exception as e:
        print(f"? create_project_service(): {e}")
        message.add_header("error", str(e))
        project_id = None

    agent.producer.asyncProduce(topic_name=topic_name, partition=str(-CREATE_PROJECT), header=message.header, payload=project_id)
    print(f"! create_project_service(): 프로젝트 생성 완료: {project_id}")

def create_project(project_name: str, candidate_columns: Dict[str, List[str]], files: Dict[str, bytes]) -> str | None:
    if not files:
        return None
    
    project_id = str(uuid.uuid4())

    project_dir = PROJECT_DIR / project_id
    project_dir.mkdir(parents=True, exist_ok=True)

    saved_files = save_files(path=project_dir, files=files)

    # 결합키 후보 컬럼이 없고, 데이터 파일이 2개 이상인 경우 결합키 후보 컬럼 자동 탐색
    if (not candidate_columns) and (len(saved_files) >= 2):
        dataframes = {
            file_name: dataframe 
            for file_name, file_content in files.items()
            if (dataframe := to_dataframe(file_content)) is not None
        }

        candidate_columns = find_candidate_columns(dataframes)

    save_project(project_id=project_id, project_info={
        "projectName": project_name,
        "files": saved_files,
        "candidateColumns": candidate_columns,
        "status": ProjectStatus.IDLE,
        "createdAt": datetime.now().isoformat()
    })

    return project_id

def save_files(path: Path, files: Dict[str, bytes]) -> List[str]:
    saved_files: List[str] = []
    for file_name, content in files.items():
        file_path = path / file_name
        try:
            with open(file_path, 'wb') as f:
                f.write(content)

            saved_files.append(file_name)
        except Exception:
            continue

    return saved_files

if __name__ == "__main__":
    with (
        open("resources/data1.csv", "rb") as f1,
        open("resources/data2.csv", "rb") as f2,
        open("resources/data3.csv", "rb") as f4,
    ):
        create_project("test_project", {}, {
            "data1.csv": f1.read(),
            "data2.csv": f2.read(),
            "data3.csv": f4.read()
        })