from concurrent.futures import ThreadPoolExecutor
import os
from threading import Event

from py_client.agent import Agent
from py_client.message.message import Message
from utils import MARKDOWN_PATH

CONVERT = 1

def convert_service(agent: Agent, topic_name: str, message: Message):
    markdown = None
    try:
        if not (file_name := message.get_header("file.name")):
            raise ValueError("file.name header is missing")
        
        if (markdown := convert_and_save(file_name=file_name, payload=message.payload)) is None:
            raise ValueError("Unsupported file format or conversion failed")
    except Exception as e:
        print(f"? convert_service(): {e}")
        message.add_header("error", str(e))

    agent.producer.asyncProduce(
        topic_name=topic_name, partition=str(-CONVERT),
        header=message.header, payload=markdown
    )

def convert_and_save(file_name: str, payload: bytes | bytearray):
    os.makedirs(MARKDOWN_PATH, exist_ok=True)
    with open(MARKDOWN_PATH / file_name, mode="wb") as f:
        f.write(payload)

    if (markdown := to_markdown(datas=payload)) is None:
        return None
    
    return markdown.encode("utf-8")
    
def to_markdown(datas: bytes) -> str | None:
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