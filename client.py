from concurrent.futures import Future
from queue import Queue
import socket
from threading import Event, Thread, Lock
from typing import Dict

from py_client.message.message import Message
from py_client.message.message_option import MessageOption
from py_client.message.message_type import MessageType
from py_client.message.message_encoder import MessageEncoder


class Client:

    def __init__(self, host: str, port: int, client_id: str):
        self.host = host
        self.port = port
        self.client_id = client_id
        self.request_id_counter = 0

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((host, port))

        self.requests: Dict[int, Future[Message]] = {} # RES_XXX 처리 용
        self.lock = Lock()
        self.subscriptions: Dict[str, Dict[int, Queue[Message]]] = {} # TOPIC_UPDATE 알림

        self.stop_event = Event()
        self.thread = Thread(target=self.__receive, daemon=True)
        self.thread.start()

    def close(self):
        self.sock.close()
        self.stop_event.set()
        self.thread.join(timeout=10)

    def __receive(self):
        from py_client.message.message_decoder import MessageDecoder
        decoder = MessageDecoder()

        while not self.stop_event.is_set():
            try:
                chunk = self.sock.recv(4096)
                if not chunk:
                    break

                message = decoder.decode(chunk)
                if message is None:
                    continue
                
                message_type = message.option_as_byte(MessageOption.MESSAGE_TYPE)
                if message_type == MessageType.TOPIC_UPDATE.value:
                    topic_name = message.option_as_str(MessageOption.TOPIC_NAME)
                    partition = message.option_as_int(MessageOption.PARTITION)
                    if (topic_name is None) or (partition is None):
                        continue

                    partition_map = self.subscriptions.get(topic_name)
                    if partition_map is None:
                        continue

                    notifiedQueue = partition_map.get(partition)
                    if notifiedQueue is None:
                        continue

                    notifiedQueue.put(message) # 알림 메시지 전달

                else:
                    request_id = message.option_as_int(MessageOption.REQUEST_ID)
                    if request_id is None: # RES_XXX 메시지인데 요청 id가 없다는 것은 command 했다는 뜻(not request)
                        continue

                    with self.lock:
                        request = self.requests[request_id]

                    if request is None:
                        continue
                    
                    # request id를 통해 요청자를 이미 찾았으므로 메시지에서 제거
                    # 요청자에게 전달하고, 요청자를 목록에서 제거
                    message.remove_options(MessageOption.REQUEST_ID)
                    request.set_result(message)
                    del self.requests[request_id]

            except Exception as e:
                print("[error] Client.__receive():", e)
                break

    def command(self, message: 'Message'):
        message.add_option(MessageOption.CLIENT_ID, self.client_id)
        self.sock.sendall(MessageEncoder.encode(message))

    def request(self, message: 'Message'):
        request_id = self.request_id_counter
        self.request_id_counter += 1

        message.add_options({
            MessageOption.CLIENT_ID: self.client_id,
            MessageOption.REQUEST_ID: request_id
        })

        future: Future[Message] = Future()
        with self.lock:
            self.requests[request_id] = future

        self.sock.sendall(MessageEncoder.encode(message))

        return future
    
    def subscribe(self, topic_name: str, partition: int, event: Event, out: Queue):
        message = Message().add_options({
            MessageOption.MESSAGE_TYPE: MessageType.REQ_SUBSCRIBE,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition
        })

        # 구독 요청
        response: Message = self.request(message).result()
        message_type = response.option_as_byte(MessageOption.MESSAGE_TYPE)
        if (message_type is None) or (message_type != MessageType.RES_SUBSCRIBE.value):
            return None

        partition_map: Dict[int, Queue] = self.subscriptions.get(topic_name, {})
        notified_queue = partition_map.get(partition, Queue())

        # 구독 알림 큐 갱신
        partition_map[partition] = notified_queue
        self.subscriptions[topic_name] = partition_map

        def worker_subscribe():
            while not event.is_set():
                try:
                    message = notified_queue.get(timeout=3) # 3초마다 빠져나옴
                    out.put(message)
                except Exception: # timeout 시 event 체크 후 재시도
                    continue

            del partition_map[partition]
            self.unsubscribe(topic_name, partition)
        
        thread = Thread(target=worker_subscribe, daemon=True)
        thread.start()

        return thread
        
    
    def unsubscribe(self, topic_name: str, partition: int):
        message = Message().add_options({
            MessageOption.MESSAGE_TYPE: MessageType.REQ_UNSUBSCRIBE,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition
        })

        self.command(message)