
from queue import Queue
import threading
from client import Client
from message.message import Message
from message.message_option import MessageOption
from message.message_type import MessageType
from utils import Utils


class Consumer:

    def __init__(self, host: str, port: int, consumer_id: str):
        Utils.validate(host, port, consumer_id)

        self.consumer_id = consumer_id
        self.client = Client(host, port)

    def __enter__(self):
      return self

    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.client.sock.close()
        return False
        
    def consume(self, topic_name: str, partition, cursor: int = None):
        Utils.validate(topic_name)

        message = Message().add_options({
            MessageOption.TYPE: MessageType.REQ_PULL,
            MessageOption.ID: self.consumer_id,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition
        })

        if (cursor is not None) and cursor >= 0:
            message.add_option(MessageOption.CURSOR, cursor)

        return self.client.request(message)
    
    def subscribeAndConsume(self, topic_name: str, partition: int, out: Queue[Message]):
        notified_queue = Queue()
        notifier_thread = self.client.subscribe(topic_name, partition, self.consumer_id, notified_queue)

        Utils.validate(notifier_thread) # 구독 실패 시 notifier_thread는 None
    
        def do():
            while True:
                try:
                    notified: Message = notified_queue.get() # blocking
                    notified_type = notified.option(MessageOption.TYPE)
                    if (notified_type is None) or (notified_type != MessageType.TOPIC_UPDATE.value):
                        continue

                    cursor = notified.option(MessageOption.CURSOR)
                    out.put(self.consume(topic_name, partition, cursor).result())

                except Exception:
                    break

        thread = threading.Thread(target=do, daemon=True)
        thread.start()

        return thread
        
    
if __name__ == "__main__":
    with Consumer("localhost", 3401, "user") as consumer:
        queue: Queue[Message] = Queue()
        consumer.subscribeAndConsume("test_topic", 0, queue)

        for _ in range(3):
            message = queue.get()
            print("type:", message.option(MessageOption.TYPE))
            print("id:", message.option(MessageOption.ID))
            print("topic_name:", message.option(MessageOption.TOPIC_NAME))
            print("partition:", message.option(MessageOption.PARTITION))
            print("cursor:", message.option(MessageOption.CURSOR))
            print("payload:", message.option(MessageOption.PAYLOAD))