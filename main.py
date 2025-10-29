from py_client.message.message_decoder import MessageDecoder
from py_client.message.message_encoder import MessageEncoder
from py_client.message.message import Message

if __name__ == "__main__":
    decoder = MessageDecoder("option_mapping_table.properties")
    msg = Message().add_options({
        "message_type": 0,
        "client_id": "user",
        "topic_name": "topic",
        "partition": 0,
        "cursor": 12,
        "offset": 34,
        "remaining_count": 100,
        "invalid_option": "hello world",
        "success": 1,
        "payload": "hello world",
        "request_id": 12345
    })

    encoded = MessageEncoder.encode("option_mapping_table.properties", msg)
    decoded = decoder.decode(encoded)

    assert decoded is not None
    print(decoded.get_options())
