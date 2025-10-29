from concurrent.futures import Future
from queue import Queue
import socket
import threading
from typing import Dict

from message.message import Message
from message.message_option import MessageOption
from message.message_type import MessageType
from utils import Utils


class Client:

    def __init__(self, host: str, port: int, file_path: str | None = None):
        self.file_path = file_path if file_path is not None else Utils.DEFAULT_MAPPING_FILE_PATH    
        self.host = host
        self.port = port
        self.request_id_counter = 0

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((host, port))

        self.requests: Dict[int, Future[Message]] = {} # RES_XXX 처리 용
        # self.requests: Dict[int, Queue[Message]] = {} # RES_XXX 처리 용
        self.lock = threading.Lock()
        self.subscriptions: Dict[str, Dict[int, Queue[Message]]] = {} # TOPIC_UPDATE 알림

        self.stop_event = threading.Event()
        self.thread = threading.Thread(target=self.__receive, daemon=True)
        self.thread.start()

    def __del__(self):
        self.stop_event.set()
        self.thread.join(timeout=10)
        self.sock.close()

    def __receive(self):
        from message.message_decoder import MessageDecoder
        decoder = MessageDecoder(self.file_path)

        while not self.stop_event.is_set():
            try:
                chunk = self.sock.recv(4096)
                if not chunk:
                    break

                message = decoder.decode(chunk)
                if message is None:
                    continue
                
                message_type = message.option(MessageOption.MESSAGE_TYPE)
                if message_type == MessageType.TOPIC_UPDATE.value:
                    topic_name = message.option(MessageOption.TOPIC_NAME)
                    partition = message.option(MessageOption.PARTITION)
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
                    request_id = message.option(MessageOption.REQUEST_ID)
                    if request_id is None: # RES_XXX 메시지인데 요청 id가 없다는 것은 command 했다는 뜻(not request)
                        continue

                    with self.lock:
                        request = self.requests[request_id]

                    if request is None:
                        continue
                    
                    request.set_result(message)
                    # request.put(message)
                    del self.requests[request_id]

            except Exception as e:
                print(f"[error] Client receive error: {e}")
                break

    def command(self, message: 'Message'):
        Utils.validate(message)

        from message.message_encoder import MessageEncoder
        self.sock.sendall(MessageEncoder.encode(self.file_path, message))

    def request(self, message: 'Message'):
        Utils.validate(message)

        request_id = self.request_id_counter
        self.request_id_counter += 1
        message.add_option(MessageOption.REQUEST_ID, request_id)

        future = Future()
        # queue: Queue[Message] = Queue()
        with self.lock:
            # self.requests[request_id] = queue
            self.requests[request_id] = future

        from message.message_encoder import MessageEncoder
        self.sock.sendall(MessageEncoder.encode(self.file_path, message))

        # return queue
        return future
    
    def subscribe(self, topic_name: str, partition: int, client_id: str, out: Queue):
        Utils.validate(out)

        subscribe_message = Message().add_options({
            MessageOption.MESSAGE_TYPE: MessageType.REQ_SUBSCRIBE,
            MessageOption.CLIENT_ID: client_id,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition
        })

        # 구독 요청
        subscribe_response: Message = self.request(subscribe_message).result()
        # subscribe_response: Message = self.request(subscribe_message).get()
        response_type = subscribe_response.option(MessageOption.MESSAGE_TYPE)

        # 구독 성공 시
        if response_type == MessageType.RES_SUBSCRIBE.value:
            out.put(subscribe_response) # 구독 성공 메시지 전달(cursor, remainig_count 등이 있음)

            partition_map: Dict[int, Queue] = self.subscriptions.get(topic_name, {})
            queue = partition_map.get(partition, Queue())

            partition_map[partition] = queue
            self.subscriptions[topic_name] = partition_map

            def worker_subscribe():
                while True:
                    try:
                        message = queue.get() # blocking
                        out.put(message)
                    except Exception:
                        break

                del partition_map[partition]
                self.unsubscribe(topic_name, partition, client_id)
            
            thread = threading.Thread(target=worker_subscribe, daemon=True)
            thread.start()

            return thread
        
        return None
    
    def unsubscribe(self, topic_name: str, partition: int, client_id: str):
        Utils.validate(topic_name, partition, client_id)

        unsubscribe_msg = Message().add_options({
            MessageOption.MESSAGE_TYPE: MessageType.REQ_UNSUBSCRIBE,
            MessageOption.CLIENT_ID: client_id,
            MessageOption.TOPIC_NAME: topic_name,
            MessageOption.PARTITION: partition
        })

        self.command(unsubscribe_msg)