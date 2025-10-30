from py_client.utils import Utils
from py_client.message.message import Message

class Producer:

    def __init__(self, host: str, port: int, producer_id: str, file_path: str | None = None):
        Utils.validate(host, port, producer_id)

        self.producer_id = producer_id

        from py_client.client import Client
        self.client = Client(host, port, file_path)

    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.client.sock.close()
        return False

    def __createMessage(self, topic_name: str, partition: int, payload: bytes):
        from py_client.message.message_option import MessageOption    
        from py_client.message.message_type import MessageType

        return Message().add_options({
            MessageOption.MESSAGE_TYPE: MessageType.REQ_PUSH,
            MessageOption.CLIENT_ID: self.producer_id,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition,
            MessageOption.PAYLOAD: payload
        })
    
    def asyncProduce_(self, message: Message):
        Utils.validate(message)

        self.client.command(message)
    
    def asyncProduce(self, topic_name: str, partition: int, payload: bytes):
        Utils.validate(topic_name, partition, payload)

        message = self.__createMessage(topic_name, partition, payload)
        self.asyncProduce_(message)

    def syncProduce_(self, message: Message):
        Utils.validate(message)

        return self.client.request(message).result()
    
    def syncProduce(self, topic_name: str, partition: int, payload: bytes):
        Utils.validate(topic_name, partition, payload)

        message = self.__createMessage(topic_name, partition, payload)
        return self.syncProduce_(message)
    
if __name__ == "__main__":
    with Producer("localhost", 3401, "user") as producer:
        response = producer.asyncProduce("test_topic", 0, b"Hello World!")
        print("Response received:", response)