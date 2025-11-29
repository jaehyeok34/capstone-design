
from concurrent.futures import ThreadPoolExecutor
from threading import Event

from modules.md_to_x_converter import convert_markdown_to_format
from py_client.agent import Agent
from py_client.message.message import Message
from utils import DIR_PATH

EXPORTER = 2
def run_export(agent: Agent, topic_name: str, executor: ThreadPoolExecutor, stop: Event):
    def handler(msg: Message):
        print("! export 시작")
        try:
            format = msg.get_header("file.format", "")
            file_name = msg.get_header("file.name", "")
            if not file_name:
                raise ValueError("file.name 없음")
            
            exported = export(format=format, file_name=file_name)
            agent.producer.asyncProduce(
                topic_name=topic_name, partition=str(-EXPORTER),
                header=msg.header, payload=exported
            )
            
        except Exception as e:
            header = msg.header | {"error": str(e)}
            agent.producer.asyncProduce(topic_name=topic_name, partition=str(-EXPORTER), header=header)

        print("! export 종료")

    while not stop.is_set():
        consumed = agent.consumer.consume(topic_name=topic_name, partition=str(EXPORTER))[0]
        if not consumed.get_header("ok", "false").lower() == "true":
            print("! consumed.header.ok: false")
            continue
        
        executor.submit(handler, consumed)

def export(format: str, file_name: str):
    with open(DIR_PATH / file_name, "rb") as f:
        content = f.read()
        try:
            output_path = convert_markdown_to_format(content.decode("utf-8"), format)
            with open(output_path, "rb") as out:
                return out.read()
            
        except Exception:
            return content