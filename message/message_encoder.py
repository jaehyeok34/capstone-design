import struct

from py_client.message.message import Message

class MessageEncoder:

    @staticmethod
    def encode(file_path: str, message: 'Message'):
        e = 'utf-8'
        
        from py_client.utils import Utils
        props = Utils.read_properties(file_path)

        encoded_options = bytearray()
        for key, option in message.options.items():
            # option을 bytes로 변환
            if not isinstance(option, bytes):
                option = str(option).encode(e) 

            if key not in props: # unknown option 처리
                key = key.encode(e)
                encoded_options.extend(struct.pack('b', Utils.UNKNOWN_OPTION_TYPE)) # type
                encoded_options.extend(struct.pack('>I', len(key))) # key length
                encoded_options.extend(key) # key value
                encoded_options.extend(struct.pack('>I', len(option))) # option length
                encoded_options.extend(option) # option value
            
            else: # known option 처리
                option_type = props[key]
                encoded_options.extend(struct.pack('B', int(option_type))) # type
                encoded_options.extend(struct.pack('>I', len(option))) # length
                encoded_options.extend(option) # value

        total_length = len(encoded_options)
        return struct.pack('>I', Utils.MAGIC) + struct.pack('>Q', total_length) + encoded_options







