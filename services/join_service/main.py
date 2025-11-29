from concurrent.futures import ThreadPoolExecutor
import threading
from py_client.agent import Agent
from services.create_ci_service import CREATE_CI, create_ci_service
from services.create_project_service import CREATE_PROJECT, create_project_service
from services.find_candidate_column_service import FIND_CANDIDATE_COLUMN, find_candidate_column_service
from services.get_projects_service import GET_PROJECT, GET_PROJECTS, get_project_service, get_projects_service
from services.join_service import GET_PSEUDONYMIZED_FILE, JOIN, get_pseudonymized_file_service, join_service
from utils import consume


def main():
    host, port = "localhost", 3401
    client_id = "join_service"
    topic_name = "join"
    stop = threading.Event()

    with (
        Agent.of(host=host, port=port, client_id=client_id) as agent,
        ThreadPoolExecutor(max_workers=10) as executor
    ):
        service_infos = [
            (FIND_CANDIDATE_COLUMN, find_candidate_column_service), # 1
            (CREATE_PROJECT, create_project_service), # 2
            (GET_PROJECTS, get_projects_service), # 3
            (CREATE_CI, create_ci_service), # 4
            (JOIN, join_service), # 5
            (GET_PROJECT, get_project_service), # 6
            (GET_PSEUDONYMIZED_FILE, get_pseudonymized_file_service), # 7
        ]

        for partition, service in service_infos:
            executor.submit(consume, agent, topic_name, partition, executor, stop, service)

        try:
            threading.Event().wait()
        except Exception:
            print('[debug] main(): 종료')
        finally:
            stop.set()

if __name__ == '__main__':
    main()