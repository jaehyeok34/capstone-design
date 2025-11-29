import json

from py_client.agent import Agent
from py_client.message.message import Message
from utils import get_project_information, load_projects

GET_PROJECTS = 3
GET_PROJECT = 6

def get_projects_service(agent: Agent, topic_name: str, message: Message):
    if (projects := get_projects()) is None:
        message.add_header("error", "프로젝트 로드 실패")

    agent.producer.asyncProduce(
        topic_name=topic_name, partition=str(-GET_PROJECTS),
        header=message.header, payload=projects
    )
    
    print(f"! get_projects_service(): 프로젝트 목록 전송 완료")

def get_project_service(agent: Agent, topic_name: str, message: Message):
    project = None
    try:
        if not (project_id := message.get_header("project.id")):
            raise Exception("project id 누락")
        
        if (project := get_project(project_id)) is None:
            raise Exception("프로젝트 로드 실패")
    except Exception as e:
        print(f"? get_project_service(): {e}")
        message.add_header("error", str(e))
    
    agent.producer.asyncProduce(
        topic_name=topic_name, partition=str(-GET_PROJECT),
        header=message.header, payload=project
    )

    print(f"! get_project_service(): 완료")

def get_projects() -> str:
    return json.dumps(load_projects())

def get_project(project_id: str) -> str | None:
    if (information := get_project_information(project_id)):
        return json.dumps(information)
    
    return None