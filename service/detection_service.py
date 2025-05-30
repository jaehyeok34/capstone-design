import random
from typing import List

from api_gateway_utils import request_get_columns


def detect(dataset_info: str) -> List[str]:
    try:
        columns = __get_columns(dataset_info)
        if not columns:
            raise Exception('컬럼명이 비어있습니다. 데이터셋 정보를 확인해주세요.')
        
        # 임시 구현: 랜덤하게 컬럼명 중 일부를 식별정보로 간주
        detected = random.sample(columns, k=random.randint(1, len(columns)))

        # api gateway에 event 전송 코드 구현해야 됨
        return detected

    except Exception as e:
        raise Exception('detect() 실패:', e)


def __get_columns(dataset_info: str) -> List[str]:
    try:
        columns:List[str] = request_get_columns(dataset_info)
        return columns

    except Exception as e:
        raise Exception('__get_columns() 실패:', str(e))