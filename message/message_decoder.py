import struct
from .message import Message
from .message_option import MessageOption
from ..utils import Utils


class MessageDecoder:

    def __init__ (self):
        self.buffer = bytearray()
        self.state = 0 # read magic
        self.length = 0
    
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
        
        self.length = struct.unpack('>Q', self.buffer[:8])[0]
        self.buffer = self.buffer[8:]
        self.state = 2 # read message
        return True
    
    def __read_message(self):
        if len(self.buffer) < self.length:
            return None
        
        msg_bytes = self.buffer[:self.length]
        self.buffer = self.buffer[self.length:]

        message = Message()
        offset = 0

        message.add_option(MessageOption.TYPE, msg_bytes[offset]) 
        offset += 1

        id_length = struct.unpack(">I", msg_bytes[offset : offset + 4])[0]
        offset += 4

        message.add_option(MessageOption.ID, msg_bytes[offset : offset + id_length].decode('utf-8'))
        offset += id_length

        topic_name_length = struct.unpack(">I", msg_bytes[offset : offset + 4])[0]
        offset += 4

        message.add_option(MessageOption.TOPIC_NAME, msg_bytes[offset : offset + topic_name_length].decode('utf-8'))
        offset += topic_name_length

        message.add_option(MessageOption.PARTITION, struct.unpack(">i", msg_bytes[offset : offset + 4])[0])
        offset += 4

        message.add_option(MessageOption.CURSOR, struct.unpack(">q", msg_bytes[offset : offset + 8])[0])
        offset += 8

        payload_length = struct.unpack(">i", msg_bytes[offset : offset + 4])[0]
        offset += 4

        if payload_length > 0:
            message.add_option(MessageOption.PAYLOAD, bytes(msg_bytes[offset : offset + payload_length]))
            offset += payload_length

        self.state = 0
        return message

