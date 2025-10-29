
from queue import Queue
from threading import Event
from message.message import Message
from message.message_option import MessageOption
from message.message_type import MessageType
from utils import Utils


class Consumer:

    def __init__(self, host: str, port: int, consumer_id: str, file_path: str | None = None):
        Utils.validate(host, port, consumer_id)

        self.consumer_id = consumer_id
        
        from client import Client
        self.client = Client(host, port, file_path)

    def __enter__(self):
      return self

    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.client.sock.close()
        return False
        
    def consume(self, topic_name: str, partition, cursor: int | None = -1):
        Utils.validate(topic_name)

        message = Message().add_options({
            MessageOption.MESSAGE_TYPE: MessageType.REQ_PULL,
            MessageOption.CLIENT_ID: self.consumer_id,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition
        })

        if (cursor is not None) and cursor >= 0:
            message.add_option(MessageOption.CURSOR, cursor)

        # return self.client.request(message).get()
        return self.client.request(message).result()
    
    def subscribeAndConsume(self, isAllConsume: bool, topic_name: str, partition: int, event: Event, out: Queue[Message]):
        notified_queue = Queue()
        notifier = self.client.subscribe(topic_name, partition, self.consumer_id, notified_queue)

        Utils.validate(notifier) # 구독 실패 시 notifier_thread는 None
    
        def worker_consume():
            while not event.is_set():
                try:
                    notified: Message = notified_queue.get() # blocking

                    cursor = notified.option(MessageOption.CURSOR)
                    remaining_count = notified.option(MessageOption.REMAINING_COUNT)

                    if isAllConsume:
                        while (
                            (remaining_count is not None) and
                            (remaining_count > 0) and 
                            (cursor is not None) and
                            (cursor < remaining_count)
                        ):
                            consumed = self.consume(topic_name, partition) # 항상 최신 데이터 읽기
                            cursor = consumed.option(MessageOption.CURSOR)
                            remaining_count = consumed.option(MessageOption.REMAINING_COUNT)

                            out.put(consumed)

                    else:
                        out.put(self.consume(topic_name, partition, cursor))

                except Exception:
                    break

        from threading import Thread
        thread = Thread(target=worker_consume, daemon=True)
        thread.start()

        return thread
        
    
if __name__ == "__main__":
    with Consumer("localhost", 3401, "user") as consumer:
        queue: Queue[Message] = Queue()
        event = Event()
        consumer.subscribeAndConsume(True, "test_topic", 0, event, queue)

        print("구독 시작")
        for _ in range(5):
            message = queue.get()
            print("읽었음")
            print("type:", message.option(MessageOption.MESSAGE_TYPE))
            print("id:", message.option(MessageOption.CLIENT_ID))
            print("topic_name:", message.option(MessageOption.TOPIC_NAME))
            print("partition:", message.option(MessageOption.PARTITION))
            print("cursor:", message.option(MessageOption.CURSOR))
            print("payload:", message.option(MessageOption.PAYLOAD))