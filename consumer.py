
from queue import Queue
from threading import Event
from py_client.message.message import Message
from py_client.message.message_type import MessageType


class Consumer:

    def __init__(self, host: str, port: int, consumer_id: str):
        from py_client.client import Client
        self.client = Client(host, port, consumer_id)

    def __enter__(self):
      return self

    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.client.close()

    def consume(self, message: Message):
        message.type = MessageType.REQ_PULL
        return self.client.fetch(message).result()
    
    def find(self, message: Message):
        message.type = MessageType.REQ_FIND
        response = self.client.fetch(message).result()
        return int(response.get_header("offset", "-1"))
    
    def seek(self, message: Message):
        message.type = MessageType.REQ_SEEK
        response = self.client.fetch(message).result()
        return response.get_header("ok", "false").lower() == "true"