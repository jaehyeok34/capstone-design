from typing import Dict, List
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

    def consume(self, topic_name: str, partition: str, count: int = 1, timeout: int = 30 * 1000):
        message = Message().set_type(MessageType.REQ_PULL) \
            .set_topic_name(topic_name) \
            .set_partition(partition) \
            .set_count(count) \
            .set_timeout(timeout)
        
        responses: List[Message] = []
        for future in self.client.fetch(message):
            try:
                responses.append(future.result())
            except Exception as e:
                print("! Consumer.consume(): future 에러 발생:", e)

        return responses
    
    def find(self, topic_name: str, partition: str, condition: Dict[str, str], timeout: int = 30 * 1000):
        message = Message().set_type(MessageType.REQ_FIND) \
            .set_topic_name(topic_name) \
            .set_partition(partition) \
            .add_condition(condition) \
            .set_timeout(timeout)
        
        try:
            response = self.client.fetch(message)[0].result()
            return int(response.get_header("offset", "-1"))
        except Exception as e:
            print("! Consumer.find(): future 에러 발생:", e)
            return -1
        
    def seek(self, topic_name: str, partition: str, offset: int):
        message = Message().set_type(MessageType.REQ_SEEK) \
            .set_topic_name(topic_name) \
            .set_partition(partition) \
            .set_offset(offset)
        
        try:
            response = self.client.fetch(message)[0].result()
            if error := response.get_header("error"):
                raise ValueError(error)
            
            return True
        except Exception as e:
            print("! Consumer.seek(): future 에러 발생:", e)
            return False