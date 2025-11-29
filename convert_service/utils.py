from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from threading import Event
from typing import Callable

from py_client.agent import Agent
from py_client.message.message import Message

MARKDOWN_PATH = Path("./markdowns/")

def consume(agent: Agent, topic_name: str, partition: int, executor: ThreadPoolExecutor, stop: Event, handler: Callable[[Agent, str, Message], None]):
    while not stop.is_set():
        consumed = agent.consumer.consume(topic_name=topic_name, partition=str(partition))[0]
        if error := consumed.get_header("error"):
            # print(f"? consume(): {error}")
            continue

        executor.submit(handler, agent, topic_name, consumed)