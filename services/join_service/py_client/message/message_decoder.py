import struct
from typing import List

from py_client.message.message import Message
from py_client.utils import Utils


class MessageDecoder:
    def __init__ (self):
        self.buffer = bytearray()
        self.total_length = 0
        self.state = 0 # 0: magic, 1: length, 2: message
    
    def decode(self, data: bytes):
        self.buffer.extend(data)
        out: List[Message] = []

        while True:
            if self.state == 0: # read magic
                if not self.__read_magic():
                    break
            
            elif self.state == 1: # read length
                if not self.__read_length():
                    break

            elif self.state == 2: # read message
                message = self.__read_message()
                if message is None: # 충분한 데이터가 없는 경우
                    break

                out.append(message)

        return out

    def __read_magic(self):
        while len(self.buffer) >= 4:
            magic = struct.unpack('>I', self.buffer[:4])[0]
            if magic != Utils.MAGIC:
                self.buffer.pop(0)
                continue

            self.buffer = self.buffer[4:]
            self.state = 1 # read length
            return True

        return False
    
    def __read_length(self):
        if len(self.buffer) < 8:
            return False
        
        self.total_length = struct.unpack('>Q', self.buffer[:8])[0]
        self.buffer = self.buffer[8:]
        self.state = 2 # read message
        return True

    def __read_message(self):
        if len(self.buffer) < self.total_length:
            return None
        
        buf = self.buffer[:self.total_length] # total_length만큼 slice
        self.buffer = self.buffer[self.total_length:] # 남은 부분 다시 할당
        
        message = Message()
        offset = 0

        message.type = struct.unpack(">B", buf[offset : offset + 1])[0]
        offset += 1

        header_count = struct.unpack(">B", buf[offset : offset + 1])[0]
        offset += 1

        for _ in range(header_count):
            key_length = struct.unpack(">H", buf[offset : offset + 2])[0]
            offset += 2

            key = buf[offset : offset + key_length].decode('utf-8')
            offset += key_length

            value_length = struct.unpack(">I", buf[offset : offset + 4])[0]
            offset += 4

            value = buf[offset : offset + value_length].decode('utf-8')
            offset += value_length

            message.add_header(key, value)

        # payload 읽기
        if (len(buf) - offset) >= 4:
            payload_length = struct.unpack(">I", buf[offset : offset + 4])[0]
            offset += 4

            if (payload_length > 0) and ((len(buf) - offset) >= payload_length):
                message.payload = buf[offset : offset + payload_length]
                offset += payload_length

        self.state = 0 # read magic
        return message