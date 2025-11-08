import struct

from py_client.message.message import Message


class MessageEncoder:

    @staticmethod
    def encode(message: 'Message'):
        def write(encoded: bytearray, value: bytes, is_key: bool = False):
            # key는 2byte length, value는 4byte length
            fmt = '>H' if is_key else '>I'
            encoded.extend(struct.pack(fmt, len(value))) # length
            encoded.extend(value) # value

        encoded = bytearray()
        for key, value in message.get_options().items():
            # key 추가
            write(encoded, key.encode('utf-8'), True)

            # value 추가
            if not isinstance(value, (bytes, bytearray)):
                value = str(value).encode('utf-8')

            write(encoded, value)

        total_length = len(encoded)
        from py_client.utils import Utils
        return struct.pack('>I', Utils.MAGIC) + struct.pack('>Q', total_length) + encoded







