import struct
from typing import Any, Dict


class Message:

    def __init__(self):
        self.options: Dict[str, Any] = {}

    def add_option(self, key: str, value: Any):
        self.options[key] = value
        return self

    def add_options(self, options: Dict[str, Any]):
        for key, value in options.items():
            self.options[key] = value

        return self
    
    def remove_options(self, *keys: str):
        for key in keys:
            if key in self.options:
                del self.options[key]

        return self
    
    def option(self, key: str):
        return self.options.get(key, None)
    
    def option_as_str(self, key: str):
        value = self.options.get(key, None)
        if isinstance(value, str):
            return value
        elif isinstance(value, (bytes, bytearray)):
            return value.decode('utf-8')
        else:
            return None
    
    def option_as_int(self, key: str):
        value = self.options.get(key, None)
        if isinstance(value, int):
            return value
        elif isinstance(value, (bytes, bytearray)):
            return int(value.decode('utf-8'))
        else:
            return None

    def option_as_byte(self, key: str):
        return self.option_as_int(key)
    
    def option_as_bytes(self, key: str):
        value = self.options.get(key, None)
        if isinstance(value, (bytes, bytearray)):
            return value
        elif value is None:
            return None
        else:
            return str(value).encode('utf-8')
    
    def get_options(self):
        return self.options
    
    def clear(self):
        self.options.clear()
        return self
    
    def count(self) -> int:
        return len(self.options)
    
    def copy(self):
        return Message().add_options(self.options)