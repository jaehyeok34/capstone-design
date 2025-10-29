import struct
from utils import Utils

class MessageDecoder:

    def __init__ (self, file_path: str):
        self.file_path = file_path
        self.buffer = bytearray()
        self.state = 0 # read magic
        self.total_length = 0
    
    def decode(self, data: bytes):
        self.buffer.extend(data)

        while True:
            if self.state == 0: # read magic
                if not self.__read_magic():
                    break
            
            elif self.state == 1: # read length
                if not self.__read_length():
                    break

            elif self.state == 2: # read message
                message = self.__read_message()
                if message is None:
                    break

                return message

        return None
    
    def __read_magic(self):
        while len(self.buffer) >= 4:
            magic = struct.unpack('>I', self.buffer[:4])[0]
            if magic == Utils.MAGIC:
                self.buffer = self.buffer[4:]
                self.state = 1 # read length
                return True
            
            self.buffer.pop(0)

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
        
        props = Utils.read_properties(self.file_path)
        props = {v: k for k, v in props.items()}

        offset = 0

        from message.message import Message
        message = Message()
        while (offset < self.total_length):
            option_type = buf[offset]
            offset += 1

            try:
                key = props[str(option_type)]

            except KeyError:
                continue

            length = struct.unpack('>I', buf[offset : offset + 4])[0]
            offset += 4

            value = buf[offset : offset + length]
            offset += length

            Utils.cast_add(message, key, value)

        self.state = 0
        return message