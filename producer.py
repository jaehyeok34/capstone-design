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

    def asyncProduce(self, message: Message):
        self.client.command(message)
    
    def syncProduce(self, message: Message, timeout: float | None = None):
        return self.client.request(message).result(timeout)
        