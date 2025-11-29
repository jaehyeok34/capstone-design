from concurrent.futures import ThreadPoolExecutor
import os
from threading import Event

from py_client.agent import Agent
from py_client.message.message import Message
from utils import DIR_PATH

CONVERTER = 1

def run_convert(agent: Agent, topic_name: str, executor: ThreadPoolExecutor, stop: Event):
    def handler(msg: Message):
        print("! convert 시작")
        try:
            payload = msg.payload
            file_name = msg.get_header("file.name", "")
            if (payload is None) or (len(payload) == 0) or (not file_name):
                raise ValueError("필수 옵션 누락")
            
            markdown = convert_and_save(file_name=file_name, payload=payload)
            if markdown is None:
                raise ValueError("변환 실패")
            
            agent.producer.asyncProduce(
                topic_name=topic_name, partition=str(-CONVERTER),
                header=msg.header, payload=markdown
            )
        except Exception as e:
            header = msg.header | {"error": str(e)}
            agent.producer.asyncProduce(topic_name=topic_name, partition=str(-CONVERTER), header=header)
            
        print("! convert 종료")

    while not stop.is_set():
        consumed = agent.consumer.consume(topic_name=topic_name, partition=str(CONVERTER))[0]
        if not consumed.get_header("ok", "false").lower() == "true":
            print("! consumed.header.ok: false")
            continue
        
        executor.submit(handler, consumed)

def convert_and_save(file_name: str, payload: bytes | bytearray):
    os.makedirs(DIR_PATH, exist_ok=True)
    with open(DIR_PATH / file_name, mode="wb") as f:
        f.write(payload)

    markdown = to_markdown(datas=payload)
    if markdown is None:
        return None
        
    return markdown.encode("utf-8")
    
def to_markdown(datas: bytes):
    from io import BytesIO
    import pandas as pd

    buf = BytesIO(datas)
    try:
        buf.seek(0)
        return pd.read_csv(buf).to_markdown()
    
    except Exception as e:
        print(f"[debug] read_csv(): {e}")
        pass

    try:
        buf.seek(0)
        return pd.read_excel(buf).to_markdown()
    
    except Exception as e:
        print(f"[debug] read_excel(): {e}")
        pass

    try:
        buf.seek(0)
        return pd.read_json(buf).to_markdown()
    
    except Exception as e:
        print(f"[debug] read_json(): {e}")
        pass

    return None