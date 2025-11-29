from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
from threading import Event
from typing import Any, Callable, Dict

from py_client.agent import Agent
from py_client.message.message import Message

class ProjectStatus():
    IDLE = "idle"
    ACTIVE = "active"
    DONE = "done"

PROJECT_DIR = Path('./projects/')
CI_FILE_NAME = "connecting_information.csv"
JOINED_FILE_NAME = "joined.csv"
PSEUDONYMIZED_FILE_NAME = "pseudonymized.csv"

def consume(agent: Agent, topic_name: str, partition: int, executor: ThreadPoolExecutor, stop: Event, handler: Callable[[Agent, str, Message], None]):
    while not stop.is_set():
        consumed = agent.consumer.consume(topic_name=topic_name, partition=str(partition))[0]
        if error := consumed.get_header("error"):
            # print(f"? consume(): {error}")
            continue

        executor.submit(handler, agent, topic_name, consumed)

def to_dataframe(datas: bytes):
    from io import BytesIO
    import pandas as pd

    buf = BytesIO(datas)
    try:
        buf.seek(0)
        return pd.read_csv(buf)
    
    except Exception as e:
        print(f"[debug] read_csv(): {e}")
        pass

    try:
        buf.seek(0)
        return pd.read_excel(buf)
    
    except Exception as e:
        print(f"[debug] read_excel(): {e}")
        pass

    try:
        buf.seek(0)
        return pd.read_json(buf)
    
    except Exception as e:
        print(f"[debug] read_json(): {e}")
        pass

    return None

def save_project(project_id: str, project_info: Dict[str, Any]) -> bool:
    projects_path = PROJECT_DIR / 'projects.json'

    projects = load_projects()
    projects[project_id] = project_info
    try:
        with open(projects_path, "w", encoding="utf-8") as f:
            json.dump(projects, f, ensure_ascii=False, indent=4)

        return True
    except Exception as e:
        print(f"? save_project(): {e}")

    return False

def load_projects() -> Dict[str, Dict[str, Any]]:
    projects_path = PROJECT_DIR / 'projects.json'
    try:
        with open(projects_path, 'r', encoding='utf-8') as f:
            projects = json.load(f)

        return projects
    except Exception as e:
        print(f"? load_projects(): {e}")
    
    return defaultdict(dict)

def add_project_option(project_id: str, key: str, value: Any) -> bool:
    if (projects := get_project_information(project_id)) is None:
        return False
    
    projects[key] = value
    return save_project(project_id, projects)
    
def get_project_information(project_id: str):
    return load_projects().get(project_id, None)