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

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((host, port))

        self.requests: Dict[str, List[Future[Message]]] = {}
        self.lock = Lock()

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

                self.__channel_read(message)

            except Exception as e:
                print("[error] Client.__receive():", e)
                break

        
    def __channel_read(self, message: Message):
        request_id = message.get_header('request.id')
        if not request_id:
            return
        
        with self.lock:
            futures = self.requests.get(request_id)

        if futures is None:
            return

        message.remove_header('request.id')
        for future in futures:
            if not future.done():
                future.set_result(message)
                break

        if all(future.done() for future in futures):
            with self.lock:
                del self.requests[request_id]


    def fetch(self, message: Message):
        request_id = str(self.request_id_counter)
        self.request_id_counter += 1

        message.add_header_dict({
            "client.id": self.client_id,
            "request.id": request_id
        })

        count = int(message.get_header("count", "1"))
        futures: List[Future[Message]] = []
        for _ in range(count):
            future: Future[Message] = Future()
            futures.append(future)

        with self.lock:
            self.requests[request_id] = futures

        self.sock.sendall(MessageEncoder.encode(message))

        return futures