from typing import Dict, List, Tuple

from api_gateway_utils import get_cardinality_ratio


def cardinality_ratio(dataset_info: str, columns: List[Tuple[str, str]]):
    pii, non_pii = [], []
    for column in columns:
        ratio = get_cardinality_ratio(dataset_info, column[0])
        if not ratio:
            raise Exception(f'cardinality_ratio() 실패: {column[0]}의 카디널리티 비율을 가져오지 못했습니다.')
        
        if ratio >= 0.95:
            pii.append(column)
            continue

        non_pii.append(column)

    return pii, non_pii
        