import re
from typing import List, Tuple
from service.db_utils import get_metadata, get_standard_term


def domain_dict(columns: List[str]) -> Tuple[List[Tuple[str, str]], List[Tuple[str, str]]]:
    # [(원본, 표준화), (원본, 표준화), ...]
    pii, non_pii  = [], []
    normalized = __normalization(columns)
    
    for i, col in enumerate(normalized):
        std_term = get_standard_term(col) # ex: 주거지 -> 주소
        if not std_term:
            non_pii.append((columns[i], col))
            continue
        
        pii.append((columns[i], col))

    return pii, non_pii

def __normalization(columns: List[str]) -> List[str]:
    """컬럼명을 전처리하여 소문자화 및 특수문자 제거"""
    cleaned = []
    for col in columns:
        col = col.strip().lower()  # 공백 제거, 소문자 통일
        col = re.sub(r'[^가-힣a-zA-Z0-9]', '', col)  # 특수문자 제거
        cleaned.append(col)

    return cleaned