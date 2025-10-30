from typing import Any, Dict
from py_client.utils import Utils


class Message:

    def __init__(self):
        self.options: Dict[str, Any] = {}

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
    
    def get_options(self):
        return self.options
    
    def remove_option(self, key: str):
        Utils.validate(key)

        if key in self.options:
            del self.options[key]

        return self
    
    def remove_options(self, *keys: str):
        Utils.validate(keys)

        for key in keys:
            if key in self.options:
                del self.options[key]

        return self
    
    def count(self) -> int:
        return len(self.options)
    
    def clear(self):
        self.options.clear()

        return self
    
    def copy(self):
        return Message().add_options(self.options)