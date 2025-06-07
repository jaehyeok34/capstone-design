import hashlib
import os
import tempfile

from flask import json
from api_gateway_utils import get_all_values, publish_event, register_csv
from dto.matching_key_reqeust import MatchingKeyRequest


def generate_matching_key(matching_key_request: MatchingKeyRequest):
    dataset_info_list = matching_key_request.dataset_info_list
    pii = matching_key_request.pii

    def hash_row(row):
        concatenated = "".join(row.astype(str))
        return hashlib.sha256(concatenated.encode('utf-8')).hexdigest()

    new_dataset_info_list = []
    with tempfile.TemporaryDirectory() as temp_dir:
        for dataset_info in dataset_info_list:
            all_values = get_all_values(dataset_info)
            if (all_values is None) or (all_values.empty):
                raise Exception(f'{dataset_info}의 데이터를 찾을 수 없습니다.')
            
            all_values['mk_serial_number'] = [f"{dataset_info}_{i+1}" for i in range(len(all_values))]
            all_values['matching_key'] = all_values[pii].apply(hash_row, axis=1)

            file_name = f"mk_{dataset_info}.csv"
            path = os.path.join(temp_dir, file_name)

            all_values.to_csv(path, index=False)
            new = register_csv(path)
            if new is None:
                raise Exception(f'{dataset_info} 결합키 생성 실패')
            
            new_dataset_info_list.append(new)

    publish_event(
        name='matching-key.generate.success',
        json_data=json.dumps(new_dataset_info_list)
    )