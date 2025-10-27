from enum import IntEnum, auto


class MessageType(IntEnum):
    REQ_PULL        = 0
    RES_PULL        = 1
    REQ_PUSH        = 2
    RES_PUSH        = 3
    REQ_SUBSCRIBE   = 4
    RES_SUBSCRIBE   = 5
    REQ_UNSUBSCRIBE = 6
    RES_UNSUBSCRIBE = 7
    TOPIC_UPDATE    = 8