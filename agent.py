from queue import Queue
import threading
from typing import Any, Callable
from py_client.consumer import Consumer
from py_client.message.message import Message
from py_client.message.message_option import MessageOption
from py_client.message.message_type import MessageType
from py_client.producer import Producer


class Agent:

    def __init__(self, producer: Producer, consumer: Consumer):
        self.producer = producer
        self.consumer = consumer

    def request(self, produce_message: Message, consume_message: Message, timeout: float | None = None):
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

    def respond(self, consume_message: Message, produce_message: Message, handler: Callable[[Message], bytes], timeout: float | None = None):
        notified_queue = Queue()
        stop_event = threading.Event()
        notifier = self.consumer.subscribe(consume_message, stop_event, notified_queue, timeout)
        if notifier is None:
            return
        
        try:
            notified = notified_queue.get(timeout=timeout)
            consumed = self.consumer.consume(notified, timeout)
            if consumed is None:
                raise Exception("consume 실패")
            
            response = handler(consumed)
            produce_message.add_option(MessageOption.PAYLOAD, response)
            produced = self.producer.syncProduce(produce_message, timeout)

            return produced.option_as_byte(MessageOption.OK) != 0
        
        except Exception as e:
            print("Agent.respond():", e)

        finally:
            stop_event.set()
            notifier.join()
