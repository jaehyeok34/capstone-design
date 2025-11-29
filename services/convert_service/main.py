from concurrent.futures import ThreadPoolExecutor
from threading import Event

from convert import CONVERT, convert_service
from export import EXPORT, export_service
from py_client.agent import Agent
from utils import consume


def main():
    host, port = 'localhost', 3401
    client_id = "converter"
    topic_name = "convert"
    stop = Event()
    
    with (
        Agent.of(host=host, port=port, client_id=client_id) as agent,
        ThreadPoolExecutor(max_workers=10) as executor
    ):
        service_informations = [
            (CONVERT, convert_service),
            (EXPORT, export_service)
        ]

        for partition, service in service_informations:
            executor.submit(consume, agent, topic_name, partition, executor, stop, service)

        try:
            Event().wait() # main thread 대기
        except Exception:
            print("[debug] main(): 종료")
        finally:
            stop.set()

if __name__ == "__main__":
    main()