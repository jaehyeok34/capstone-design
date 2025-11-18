from enum import IntEnum

class MessageType(IntEnum):
    REQ_PULL        = 0
    RES_PULL        = 1

    REQ_PUSH        = 2
    RES_PUSH        = 3

    REQ_FIND        = 4
    RES_FIND        = 5

    REQ_SEEK        = 6
    RES_SEEK        = 7