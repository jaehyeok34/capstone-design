from concurrent.futures import Future
import socket
from threading import Event, Thread, Lock
from typing import Dict, List

from py_client.message.message import Message
from py_client.message.message_encoder import MessageEncoder


class Client:
    def __init__(self, host: str, port: int, client_id: str):
        self.host = host
        self.port = port
        self.client_id = client_id

        self.request_id_counter = 0

        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((host, port))
        self.socket_lock = Lock()

        self.requests: Dict[str, List[Future[Message]]] = {}
        self.requests_lock = Lock()

        self.stop = Event()
        self.thread = Thread(target=self.__receive, daemon=True)
        self.thread.start()

    def close(self):
        self.socket.close()
        self.stop.set()
        self.thread.join(timeout=10)

    def __receive(self):
        from py_client.message.message_decoder import MessageDecoder
        decoder = MessageDecoder()

        while not self.stop.is_set():
            try:
                chunk = self.socket.recv(4096)
                if not chunk:
                    break

                messages = decoder.decode(chunk)
                if not messages:
                    continue

                for message in messages:
                    self.__channel_read(message)

            except Exception as e:
                print("! Client.__receive():", e)
                break

        
    def __channel_read(self, message: Message):
        request_id = message.get_header('request.id')
        if not request_id:
            return
        
        with self.requests_lock:
            futures = self.requests.get(request_id)

        if futures is None:
            return
            
        message.remove_header("request.id")
        with self.requests_lock:
            futures = self.requests.get(request_id)

        if futures is None:
            return
        
        for future in futures:
            if not future.done():
                future.set_result(message)
                break

        if all(future.done() for future in futures):
            with self.requests_lock:
                del self.requests[request_id]

    def fetch(self, message: Message):
        request_id = str(self.request_id_counter)
        self.request_id_counter += 1

        message.set_client_id(self.client_id) \
            .add_header("request.id", request_id)

        count = int(message.get_header("count", "1"))
        futures: List[Future[Message]] = []
        for _ in range(count):
            future: Future[Message] = Future()
            futures.append(future)

        with self.requests_lock:
            self.requests[request_id] = futures

        with self.socket_lock:
            self.socket.sendall(MessageEncoder.encode(message))

        # 별도 스레드에서 동작하는 channel_read()에서 futures에 접근하여 
        # fetch를 호출한 스레드에서 같은 리스트에 대하여 동시 접근 문제가 발생할 수 있어
        # 얕은 복사를 통해 같은 값을 공유하는 별도의 리스트를 반환하여 동일 리스트 동시 접근 문제 해결
        return futures.copy()