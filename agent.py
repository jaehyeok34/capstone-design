from typing import Dict
from py_client.consumer import Consumer
from py_client.message.message import Message
from py_client.message.message_type import MessageType
from py_client.producer import Producer


class Agent:

    def __init__(self, producer: Producer, consumer: Consumer):
        self.producer = producer
        self.consumer = consumer

    def __enter__(self):
      return self

    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.close()

    def close(self):
        self.producer.client.close()
        self.consumer.client.close()

    def find_and_seek(self, topic_name: str, partition: str, condition: Dict[str, str], timeout: int = 30 * 1000):
        if (offset := self.consumer.find(topic_name=topic_name, partition=partition, condition=condition, timeout=timeout)) == -1:
            return False
        
        self.consumer.seek(topic_name=topic_name, partition=partition, offset=offset)
        
        return True
    
    @staticmethod
    def of(host: str, port: int, client_id: str):
        producer = Producer(host, port, client_id)
        consumer = Consumer(host, port, client_id)
        
        return Agent(producer, consumer)