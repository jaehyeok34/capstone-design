import json
from typing import Dict, List
from api_gateway_utils import get_columns, publish_event
from dto.pii_detection_response import PiiDetectionResponse
from service.cardinality_ratio import cardinality_ratio
from service.domain_dict import domain_dict
from service.embedding_model import embedding_model


def detect(dataset_info_list: List[str]) -> PiiDetectionResponse:
    detected = {}
    for dataset_info in dataset_info_list:
        columns = get_columns(dataset_info)
        if not columns:
            raise Exception(f'{dataset_info}의 컬럼명이 비어있습니다.')
        
        detected[dataset_info] = []

        pii, non_pii = domain_dict(columns)
        detected[dataset_info].extend(pii)

        pii, non_pii = embedding_model(non_pii)
        detected[dataset_info].extend(pii)

        pii, non_pii = cardinality_ratio(dataset_info, non_pii)
        detected[dataset_info].extend(pii)

        detected[dataset_info] = sorted([x[0] for x in detected[dataset_info]])

    pii = list(set.intersection(*(set(v) for v in detected.values())))  # 모든 dataset_info의 pii 값들의 교집합을 구함
    response = PiiDetectionResponse(dataset_info_list=dataset_info_list, pii=pii)
    publish_event(
        name='pii.detection.success', 
        json_data=response.model_dump_json()
    )

    return response