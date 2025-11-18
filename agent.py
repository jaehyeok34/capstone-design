from py_client.consumer import Consumer
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
    
    @staticmethod
    def of(host: str, port: int, client_id: str):
        producer = Producer(host, port, client_id)
        consumer = Consumer(host, port, client_id)
        
        return Agent(producer, consumer)