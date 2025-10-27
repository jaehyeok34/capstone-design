from concurrent.futures import Future
from queue import Queue
import socket
import threading
from typing import Dict

from message.message import Message
from message.message_decoder import MessageDecoder
from message.message_option import MessageOption
from message.message_type import MessageType
from utils import Utils


class Client:

    def __init__(self, host: str, port: int):
        self.host = host
        self.port = port

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((host, port))

        self.requests: Queue[Future[Message]] = Queue() # RES_XXX 처리
        self.subscriptions: Dict[str, Dict[int, Queue[Message]]] = {} # TOPIC_UPDATE 알림

        self.stop_event = threading.Event()
        self.thread = threading.Thread(target=self.__receive, daemon=True)
        self.thread.start()

    def __del__(self):
        self.stop_event.set()
        self.thread.join(timeout=10)
        self.sock.close()

    def __receive(self):
        decoder = MessageDecoder()
        while not self.stop_event.is_set():
            try:
                chunk = self.sock.recv(4096)
                if not chunk:
                    break

                message = decoder.decode(chunk)
                if message is None:
                    continue

                message_type = message.option(MessageOption.TYPE)
                if message_type == MessageType.TOPIC_UPDATE.value:
                    topic_name = message.option(MessageOption.TOPIC_NAME)
                    partition = message.option(MessageOption.PARTITION)

                    partition_map: Dict[int, Queue] = self.subscriptions.get(topic_name)
                    if partition_map is None:
                        continue

                    queue = partition_map.get(partition)
                    if queue is None:
                        continue

                    queue.put(message)

                else:
                    self.requests.get().set_result(message)

            except Exception:
                break

    def command(self, message: 'Message'):
        Utils.validate(message)

        self.sock.sendall(message.to_bytes())

    def request(self, message: 'Message'):
        Utils.validate(message)

        self.sock.sendall(message.to_bytes())

        future = Future()
        self.requests.put(future)

        return future
    
    def subscribe(self, topic_name: str, partition: int, client_id: str, out: Queue):
        Utils.validate(out)

        subscribe_message = Message().add_options({
            MessageOption.TYPE: MessageType.REQ_SUBSCRIBE,
            MessageOption.ID: client_id,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition
        })

        # 구독 요청
        subscribe_response: Message = self.request(subscribe_message).result()
        response_type = subscribe_response.option(MessageOption.TYPE)

        # 구독 성공 시
        if response_type == MessageType.RES_SUBSCRIBE.value:
            partition_map: Dict[int, Queue] = self.subscriptions.get(topic_name, {})
            queue = partition_map.get(partition, Queue())

            partition_map[partition] = queue
            self.subscriptions[topic_name] = partition_map

            def do():
                while True:
                    try:
                        message = queue.get() # blocking
                        out.put(message)
                    except Exception:
                        break

                del partition_map[partition]
                self.unsubscribe(topic_name, partition, client_id)
            
            thread = threading.Thread(target=do, daemon=True)
            thread.start()

            return thread
        
        return None
    
    def unsubscribe(self, topic_name: str, partition: int, client_id: str):
        Utils.validate(topic_name, partition, client_id)

        unsubscribe_msg = Message().add_options({
            MessageOption.TYPE: MessageType.REQ_UNSUBSCRIBE,
            MessageOption.ID: client_id,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition
        })

        self.command(unsubscribe_msg)