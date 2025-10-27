from typing import Any, Dict
from message.message_option import MessageOption
from utils import Utils
import struct


class Message:

    def __init__(self):
        self.options = {}

    def add_option(self, key: str, value):
        Utils.validate(key, value)

        self.options[key] = value

        return self

    def add_options(self, options: Dict[str, Any]):
        Utils.validate(options)

        for key, value in options.items():
            Utils.validate(key, value)
            
            self.options[key] = value

        return self

    def option(self, key: str):
        Utils.validate(key)

        return self.options.get(key, None)
    
    def remove_option(self, key: str):
        Utils.validate(key)

        if key in self.options:
            del self.options[key]

        return self
    
    def count(self) -> int:
        return len(self.options)

    def to_bytes(self):
        type = self.option(MessageOption.TYPE)
        id = self.option(MessageOption.ID)
        topic_name = self.option(MessageOption.TOPIC_NAME)
        partition = self.option(MessageOption.PARTITION)
        cursor = self.option(MessageOption.CURSOR)
        payload = self.option(MessageOption.PAYLOAD)

        Utils.validate(type, id, topic_name, partition) # 필수 옵션 검증
        if (cursor is None):
            cursor = -1

        # I(magic) Q(total lenth) B(type) I(id length) {}s(id) I(topic name length) {}s(topic name)
        # i(partition) q(cursor, signed long long) i(payload length, unsigned int)
        fmt = f">I Q B I{len(id)}s I{len(topic_name)}s i q i"

        options = [
            type, 
            len(id), id.encode('utf-8'),
            len(topic_name), topic_name.encode('utf-8'),
            partition,
            cursor,
            len(payload) if payload is not None else -1
        ]

        if (payload is not None):
            fmt += f"{len(payload)}s"
            options.append(payload)

        total_length = struct.calcsize(fmt) - 12 # I(magic) Q(total length) 제외
        options = [Utils.MAGIC, total_length] + options
        packed = struct.pack(fmt, *options)

        return packed