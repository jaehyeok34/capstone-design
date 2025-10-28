class Utils:
    MAGIC = 0x6B3FA0FF

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