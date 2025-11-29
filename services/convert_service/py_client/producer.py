from typing import Any, Dict
from py_client.message.message import Message
from py_client.message.message_type import MessageType

class Producer:

    def __init__(self, host: str, port: int, producer_id: str):
        self.producer_id = producer_id

        from py_client.client import Client
        self.client = Client(host, port, producer_id)

    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.client.close()

    def __produce(self, message: Message):
        message.set_type(MessageType.REQ_PUSH)
        return self.client.fetch(message)
    
    def asyncProduce(self, topic_name: str, partition: str, header: Dict[str, str], payload: Any = None):
        message = Message().set_topic_name(topic_name) \
            .add_header_dict(header) \
            .set_partition(partition) \
            .set_payload(payload)
        
        self.__produce(message)

    def syncProduce(self, topic_name: str, partition: str, header: Dict[str, str], payload: Any = None, timeout: int = 30 * 1000):
        message = Message().set_topic_name(topic_name) \
            .add_header_dict(header) \
            .set_partition(partition) \
            .set_payload(payload)
        
        return self.__produce(message)[0].result(timeout=timeout)
        