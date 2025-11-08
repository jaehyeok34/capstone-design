from py_client.message.message import Message

class Producer:

    def __init__(self, host: str, port: int, producer_id: str):
        self.producer_id = producer_id

        from py_client.client import Client
        self.client = Client(host, port, producer_id)

    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.client.close()

    def __createMessage(self, topic_name: str, partition: int, payload: bytes):
        from py_client.message.message_option import MessageOption    
        from py_client.message.message_type import MessageType

        return Message().add_options({
            MessageOption.MESSAGE_TYPE: MessageType.REQ_PUSH,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition,
            MessageOption.PAYLOAD: payload
        })
    
    def asyncProduce_(self, message: Message):
        self.client.command(message)
    
    def asyncProduce(self, topic_name: str, partition: int, payload: bytes):
        message = self.__createMessage(topic_name, partition, payload)
        self.asyncProduce_(message)

    def syncProduce_(self, message: Message):
        return self.client.request(message).result()
    
    def syncProduce(self, topic_name: str, partition: int, payload: bytes):
        message = self.__createMessage(topic_name, partition, payload)
        return self.syncProduce_(message)