
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
    
    def consume_(self, message: Message):
        return self.client.request(message).result()
        
    def consume(self, topic_name: str, partition, offset: int = -1):
        Utils.validate(topic_name)

        message = Message().add_options({
            MessageOption.MESSAGE_TYPE: MessageType.REQ_PULL,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition
        })

        if offset >= 0:
            message.add_option(MessageOption.OFFSET, offset)

        return self.consume_(message)
    
    def subscribe(self, topic_name: str, partition: int, event: Event, out: Queue[Message]):
        return self.client.subscribe(topic_name, partition, event, out)