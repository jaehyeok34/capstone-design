from concurrent.futures import ThreadPoolExecutor
from threading import Event

from convert import run_convert
from export import run_export
from py_client.agent import Agent


def main():
    host, port = 'localhost', 3401
    client_id = "converter"
    topic_name = "convert_file"
    stop = Event()
    
    with (
        Agent.of(host=host, port=port, client_id=client_id) as agent,
        ThreadPoolExecutor(max_workers=10) as executor
    ):
        for service in [run_convert, run_export]:
            executor.submit(service, agent, topic_name, executor, stop)

        try:
            Event().wait() # main thread 대기
        except Exception:
            print("[debug] main(): 종료")
        finally:
            stop.set()

if __name__ == "__main__":
    main()