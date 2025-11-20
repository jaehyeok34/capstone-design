from typing import Any, Dict

class Message:
    def __init__(self, type: int | None = None, header: Dict[str, str] | None = None, payload: Any = None):
        self.type = type
        self.header: Dict[str, str] = header if header is not None else {}
        self.payload: Any = payload

    def set_type(self, type: int):
        self.type = type
        return self
    
    def set_payload(self, payload: Any):
        self.payload = payload
        return self

    def get_header(self, key: str, default: str = ""):
        return self.header.get(key, default)

    def add_header(self, key: str, value: str):
        self.header[key] = value
        return self
    
    def add_header_dict(self, header: Dict[str, str]):
        for key, value in header.items():
            self.add_header(key, value)

        return self
    
    def remove_header(self, *keys: str):
        for key in keys:
            if key in self.header:
                del self.header[key]

        return self
    
    def remove_payload(self):
        self.payload = None
        return self
    
    def copy(self):
        return Message(self.type, self.header.copy(), self.payload)
    
    # 알려진 헤더 옵션에 대한 편의 메서드 =====
    def add_condition(self, condition: Dict[str, str]):
        for key, value in condition.items():
            self.add_header("condition." + key, value)

        return self
    
    def set_topic_name(self, topic_name: str):
        self.add_header("topic.name", topic_name)
        return self
    
    def set_partition(self, partition: int | str):
        self.add_header("partition", str(partition))
        return self

    def set_client_id(self, client_id: str):
        self.add_header("client.id", client_id)
        return self
    
    def set_timeout(self, timeout: int):
        self.add_header("timeout", str(timeout))
        return self
    
    def set_count(self, count: int):
        self.add_header("count", str(count))
        return self
    
    def set_offset(self, offset: int):
        self.add_header("offset", str(offset))
        return self