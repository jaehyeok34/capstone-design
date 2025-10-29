from utils import Utils

class Producer:

    def __init__(self, host: str, port: int, producer_id: str, file_path: str | None = None):
        Utils.validate(host, port, producer_id)

        self.producer_id = producer_id

        from client import Client
        self.client = Client(host, port, file_path)

    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.client.sock.close()
        return False

    def __createMessage(self, topic_name: str, partition: int, payload: bytes):
        from message.message import Message
        from message.message_option import MessageOption    
        from message.message_type import MessageType

        return Message().add_options({
            MessageOption.MESSAGE_TYPE: MessageType.REQ_PUSH,
            MessageOption.CLIENT_ID: self.producer_id,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition,
            MessageOption.PAYLOAD: payload
        })
    
    def asyncProduce(self, topic_name: str, partition: int, payload: bytes):
        Utils.validate(topic_name, partition, payload)

        message = self.__createMessage(topic_name, partition, payload)
        self.client.command(message)

    def syncProduce(self, topic_name: str, partition: int, payload: bytes):
        Utils.validate(topic_name, partition, payload)

        message = self.__createMessage(topic_name, partition, payload)
        response = self.client.request(message).result()

        return response
    
if __name__ == "__main__":
    with Producer("localhost", 3401, "user") as producer:
        response = producer.asyncProduce("test_topic", 0, b"Hello World!")
        print("Response received:", response)