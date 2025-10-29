import struct
from message.message import Message

class MessageEncoder:

    @staticmethod
    def encode(file_path: str, message: 'Message'):
        e = 'utf-8'
        
        from utils import Utils
        props = Utils.read_properties(file_path)

        encoded_options = bytearray()
        for key, option in message.options.items():
            if key not in props:
                print(f'[debug] 매핑 값이 존재하지 않는 옵션 키: {key}')
                continue

            mapped_key = props[key]

            if not isinstance(option, bytes):
                option = str(option).encode(e)
                
            encoded_options.extend(struct.pack('B', int(mapped_key))) # type
            encoded_options.extend(struct.pack('>I', len(option))) # length
            encoded_options.extend(option) # value

        total_length = len(encoded_options)
        return struct.pack('>I', Utils.MAGIC) + struct.pack('>Q', total_length) + encoded_options







