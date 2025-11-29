
from concurrent.futures import ThreadPoolExecutor
from threading import Event

from modules.md_to_x_converter import convert_markdown_to_format
from py_client.agent import Agent
from py_client.message.message import Message
from utils import MARKDOWN_PATH

EXPORT = 2
def export_service(agent: Agent, topic_name: str, message: Message):
    result = None
    try:
        if (not (file_name := message.get_header("file.name"))) or (not (fmt := message.get_header("file.format"))):
            raise ValueError("file.name or file.format is missing")
        
        if (result := export(fmt=fmt, file_name=file_name)) is None:
            raise ValueError(f"Unsupported format: {fmt}")
    except Exception as e:
        print(f"? export_service(): {e}")
        message.add_header("error", str(e))

    agent.producer.asyncProduce(
        topic_name=topic_name, partition=str(-EXPORT),
        header=message.header, payload=result
    )

def run_export(agent: Agent, topic_name: str, executor: ThreadPoolExecutor, stop: Event):
    def handler(msg: Message):
        print("! export 시작")
        try:
            format = msg.get_header("file.format", "")
            file_name = msg.get_header("file.name", "")
            if not file_name:
                raise ValueError("file.name 없음")
            
            exported = export(fmt=format, file_name=file_name)
            agent.producer.asyncProduce(
                topic_name=topic_name, partition=str(-EXPORT),
                header=msg.header, payload=exported
            )
            
        except Exception as e:
            header = msg.header | {"error": str(e)}
            agent.producer.asyncProduce(topic_name=topic_name, partition=str(-EXPORT), header=header)

        print("! export 종료")

    while not stop.is_set():
        consumed = agent.consumer.consume(topic_name=topic_name, partition=str(EXPORT))[0]
        if not consumed.get_header("ok", "false").lower() == "true":
            print("! consumed.header.ok: false")
            continue
        
        executor.submit(handler, consumed)

def export(fmt: str, file_name: str) -> bytes:
    with open(MARKDOWN_PATH / file_name, "rb") as f:
        content = f.read()
        if (converted := convert_markdown_to_format(content.decode("utf-8"), fmt)) is None:
            return content
        
        return converted