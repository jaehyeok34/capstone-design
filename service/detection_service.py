from typing import List
from api_gateway_utils import get_columns
from service.cardinality_ratio import cardinality_ratio
from service.domain_dict import domain_dict
from service.embedding_model import embedding_model


def detect(dataset_info: str) -> List[str]:
    try:
        columns = __get_columns(dataset_info)
        if not columns:
            raise Exception('컬럼명이 비어있습니다. 데이터셋 정보를 확인해주세요.')
        
        result = []
        # 도메인 사전
        pii, non_pii = domain_dict(columns)
        result.extend(pii)
        # print('도메인 사전 분류 결과:', pii, non_pii)

        # 임베딩 모델
        pii, non_pii = embedding_model(non_pii)
        result.extend(pii)
        # print('임베딩 모델 분류 결과:', pii, non_pii)

        # 카디널리티 비율
        pii, non_pii = cardinality_ratio(dataset_info, non_pii)
        result.extend(pii)
        print('카디널리티 비율 분류 결과:', pii, non_pii)

        return result

    except Exception as e:
        # 실패 시, 실패 event 발행 해야됨
        print('detect() 실패:', str(e))
        raise Exception('detect() 실패:', e)


def __get_columns(dataset_info: str) -> List[str]:
    try:
        columns:List[str] = get_columns(dataset_info)
        return columns

    except Exception as e:
        raise Exception('__get_columns() 실패:', str(e))