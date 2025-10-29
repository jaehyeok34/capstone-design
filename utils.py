import configparser

from py_client.message.message_option import MessageOption


class Utils:
    MAGIC = 0x6B3FA0FF
    DEFAULT_MAPPING_FILE_PATH = "option_mapping_table.properties"
    UNKNOWN_OPTION_TYPE = -1

    @staticmethod
    def validate(*args):
        for arg in args:
            if arg is None:
                raise ValueError("None")

            if isinstance(arg, str) and not arg.strip():
                raise ValueError("Empty String")
            
    @staticmethod
    def is_none(*args):
        return any((arg is None) for arg in args)
    
    @staticmethod
    def read_properties(file_path: str):
        Utils.validate(file_path)
        
        config = configparser.ConfigParser()

        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        config.read_string("[DEFAULT]\n" + content)

        props = config["DEFAULT"]
        return {key: value for key, value in props.items()}
    
    @staticmethod
    def cast_add(message, key: str, value: bytes | bytearray | str):
        Utils.validate(message, key, value)

        if isinstance(value, bytes) or isinstance(value, bytearray):
            value = value.decode('utf-8')

        numeric = [
            MessageOption.MESSAGE_TYPE,
            MessageOption.SUCCESS,
            MessageOption.PARTITION,
            MessageOption.CURSOR,
            MessageOption.OFFSET,
            MessageOption.REMAINING_COUNT,
            MessageOption.REQUEST_ID
        ]

        string = [
            MessageOption.CLIENT_ID,
            MessageOption.TOPIC_NAME,
        ]

        if key in numeric:
            message.add_option(key, int(value))
        elif key in string:
            message.add_option(key, value)
        elif key == MessageOption.PAYLOAD:
            if isinstance(value, str):
                value = value.encode('utf-8')

            message.add_option(key, value)
        else:
            return False
        
        return True