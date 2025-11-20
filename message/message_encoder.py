import struct

from py_client.message.message import Message


class MessageEncoder:
    @staticmethod
    def encode(message: 'Message'):
        encoded = bytearray()

        encoded.extend(struct.pack(">B", message.type)) # 메시지 타입 추가
        encoded.extend(struct.pack(">B", len(message.header))) # header 개수 추가

        # header 추가
        for key, value in message.header.items():
            key_bytes = key.encode('utf-8')
            value_bytes = value.encode('utf-8')

            encoded.extend(struct.pack(">H", len(key_bytes)))
            encoded.extend(key_bytes)

            encoded.extend(struct.pack(">I", len(value_bytes)))
            encoded.extend(value_bytes)

        # payload 추가(있으면)
        if message.payload:
            if isinstance(message.payload, (bytes, bytearray)):
                encoded.extend(struct.pack(">I", len(message.payload)))
                encoded.extend(message.payload)

            else:
                payload = str(message.payload).encode('utf-8')
                encoded.extend(struct.pack(">I", len(payload)))
                encoded.extend(payload)
        else:
            encoded.extend(struct.pack(">I", 0)) # payload 없음 의미

        total_length = len(encoded)
        from py_client.utils import Utils
        return struct.pack(">I", Utils.MAGIC) + struct.pack(">Q", total_length) + encoded