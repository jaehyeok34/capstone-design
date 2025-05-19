import re
from typing import List, Dict, Tuple
from db_utils import get_standard_term, get_metadata_by_standard_term
import os
import pandas as pd

def extract_columns_from_folder(folder_path: str) -> List[Dict[str, str]]:
    """
    폴더 내 모든 CSV/XLSX 파일에서 컬럼명 추출 (파일명과 컬럼명 쌍으로 저장)
    :param folder_path: 폴더 경로
    :return: [{"filename": 파일명, "column": 컬럼명}, ...] 리스트
    """
    columns_info = []
    for filename in os.listdir(folder_path):
        file_path = os.path.join(folder_path, filename)
        if filename.endswith('.csv'):
            df = pd.read_csv(file_path, nrows=0)
        elif filename.endswith('.xlsx'):
            df = pd.read_excel(file_path, nrows=0)
        else:
            continue
        for col in df.columns:
            columns_info.append({"filename": filename, "column": col})
    return columns_info

def clean_column_names(columns: List[str]) -> List[str]:
    """컬럼명을 전처리하여 소문자화 및 특수문자 제거"""
    cleaned = []
    for col in columns:
        col = col.strip().lower()  # 공백 제거, 소문자 통일
        col = re.sub(r'[^가-힣a-zA-Z0-9]', '', col)  # 특수문자 제거
        cleaned.append(col)
    return cleaned

def flatten_list(nested):
    for item in nested:
        if isinstance(item, list):
            yield from flatten_list(item)
        else:
            yield item


def process_columns(columns_info: List[Dict[str, str]]) -> Tuple[List[Dict], List[Dict]]:
    """
    컬럼명 표준화 + 메타데이터 매칭
    - outputA: 표준화 및 메타정보 포함
    - outputB: 표준화 실패 or 사전 없음
    """
    outputA = []
    outputB = []

    for item in columns_info:
        filename = item["filename"]
        original_col = item["column"]
        cleaned_col = clean_column_names([original_col])[0]
        
        std_term = get_standard_term(cleaned_col)
        if std_term:
            metadata = get_metadata_by_standard_term(std_term)
            if metadata:
                outputA.append({
                    "filename": filename,
                    "original_column": original_col,
                    "cleaned_column": cleaned_col,
                    "standard_term": std_term,
                    "category": metadata.get("category"),
                    "is_sensitive": metadata.get("is_sensitive"),
                    "language": metadata.get("language")
                })
            else:
                outputB.append({"filename": filename, "column": original_col, "cleaned_column": cleaned_col})
        else:
            outputB.append({"filename": filename, "column": original_col, "cleaned_column": cleaned_col})
    
    return outputA, outputB


if __name__ == "__main__":

    folder = 'data_files/'
    columns_info = extract_columns_from_folder(folder)
    print("추출된 컬럼명:", columns_info)

    # 전처리 전후 컬럼명 출력
    original_columns = [item["column"] for item in columns_info]
    cleaned_columns = clean_column_names(original_columns)

    print("\n=== 원본 컬럼명 리스트 ===")
    for col in original_columns:
        print(col)

    # 표준화 및 메타데이터 처리
    outputA, outputB = process_columns(columns_info)

    # # ✅ 표준화된 컬럼명만 출력
    # print("\n=== 표준화된 컬럼명 리스트 (standard_term) ===")
    # for item in outputA:
    #     print(item["standard_term"])

    # 표준화 결과 전체 출력
    print("\n[표준화된 컬럼들 / 식별정보 판단됨]")
    for item in outputA:
        print(item)

    print("\n[표준화 실패 or 식별정보 아님]")
    for item in outputB:
        print(item)