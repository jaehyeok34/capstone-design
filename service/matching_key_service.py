import hashlib
from typing import List
import pandas as pd
from api_gateway_utils import get_all_values


def generate_matching_key(dataset_info: str, columns: List[str]) -> pd.DataFrame:
    try:
        all_values: pd.DataFrame = get_all_values(dataset_info)
        if all_values is None:
            raise Exception(f"'{dataset_info}'을 찾을 수 없습니다.")
        
        pii = all_values[columns]
        if pii.empty:
            raise Exception(f"'{dataset_info}'의 식별정보 컬럼이 비어 있습니다.")
        
        def hash_row(row):
            concatenated = "".join(row.astype(str))
            return hashlib.sha256(concatenated.encode('utf-8')).hexdigest()
        
        all_values['mk_serial_number'] = [f"{dataset_info}{i+1}" for i in range(len(all_values))]
        all_values['matching_key'] = pii.apply(hash_row, axis=1)

        all_values.to_csv(f'mk_{dataset_info}.csv', index=False)

        return all_values

    except Exception as e:
        raise Exception(f'generate() 실패 {e}')