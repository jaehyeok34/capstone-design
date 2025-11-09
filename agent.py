from queue import Queue
import threading
from typing import Callable
from py_client.consumer import Consumer
from py_client.message.message import Message
from py_client.message.message_option import MessageOption
from py_client.producer import Producer


class Agent:

    def __init__(self, producer: Producer, consumer: Consumer):
        self.producer = producer
        self.consumer = consumer

    def request(self, produce_message: Message, consume_message: Message, timeout: float = 5):
        notified_queue = Queue()
        stop_event = threading.Event()
        notifier = self.consumer.subscribe(consume_message, stop_event, notified_queue, timeout)
        if notifier is None:
            return None
        
        try:
            produced = self.producer.syncProduce(produce_message, timeout)
            ok = produced.option_as_byte(MessageOption.OK)
            if (ok is None) or (not ok):
                raise Exception("produce 실패")
            
            notified: Message = notified_queue.get(timeout=timeout)
            consumed = self.consumer.consume(notified, timeout)
            if consumed is None:
                raise Exception("consume 실패")

            return consumed.option_as_bytes(MessageOption.PAYLOAD)
        
        except Exception as e:
            print("Requester.request():", e)

        finally:
            stop_event.set()
            notifier.join()

    def respond(self, consume_message: Message, produce_message: Message, handler: Callable[[Message], bytes | None], timeout: float = 5):
        notified_queue = Queue()
        stop_event = threading.Event()
        notifier = self.consumer.subscribe(consume_message, stop_event, notified_queue, timeout)
        if notifier is None:
            return
        
        while True:
            try:
                notified = notified_queue.get() # 무한 대기
                consumed = self.consumer.consume(notified, timeout) # timeout 가능
                if consumed is None:
                    continue
                
                response = handler(consumed)
                produce_message.add_option(MessageOption.PAYLOAD, response)
                self.producer.asyncProduce(produce_message)

            except TimeoutError: # timeout 무시
                pass

            except Exception as e:
                print("Agent.respond():", e)
                break

        print("Agent.respond 종료 대기..")
        stop_event.set()
        notifier.join(timeout=timeout)

    def notify(self, subscribe_message: Message, timeout: float = 5):
        notified_queue = Queue()
        stop_event = threading.Event()
        notifier = self.consumer.subscribe(message=subscribe_message, event=stop_event, out=notified_queue, timeout=timeout)
        if notifier is None:
            return (None, None, None)
        
        return (notified_queue, stop_event, notifier)