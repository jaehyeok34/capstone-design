
from queue import Queue
from threading import Event
from py_client.message.message import Message
from py_client.message.message_option import MessageOption
from py_client.message.message_type import MessageType
from py_client.utils import Utils


class Consumer:

    def __init__(self, host: str, port: int, consumer_id: str):
        Utils.validate(host, port, consumer_id)

        self.consumer_id = consumer_id
        
        from py_client.client import Client
        self.client = Client(host, port, consumer_id)

    def __enter__(self):
      return self

    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.client.close()
    
    def consume(self, message: Message, timeout: float | None = None):
        message.add_option(MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.value)
        return self.client.request(message).result(timeout)
        
    def subscribe(self, message: Message, event: Event, out: Queue[Message], timeout: float | None = None):
        return self.client.subscribe(message, event, out, timeout)