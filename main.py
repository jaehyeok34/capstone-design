# main_pipeline.py

import os
from collections import defaultdict
import pandas as pd
from domain_search import extract_columns_from_folder, process_columns
from embedding_search import load_sbert_model, load_domain_terms, find_similar_terms
from cardinality_check import analyze_dataframe

# 기본 경로 설정
INPUT_FOLDER = "data_files"  # <- 사용자 정의 경로로 수정
MODEL_PATH = "model/sbert_domain_model/"
THRESHOLD = 0.84

def main(input_folder):
    print("1. 컬럼명 추출 및 전처리 시작...")
    columns_info = extract_columns_from_folder(input_folder)
    outputA, outputB = process_columns(columns_info)

    print(f"추출된 컬럼명: {len(columns_info)}개")
    print(f"  - 표준화 성공: {len(outputA)}개")
    print(f"  - 표준화 실패: {len(outputB)}개")

    print("  - 표준화 실패 컬럼:", [item["column"] for item in outputB])

    print("\n=== 원본 컬럼명 리스트 ===")
    for item in columns_info:
        print(f"{item['filename']}: {item['column']}")

    print("\n=== 표준화된 컬럼명 리스트 (standard_term) ===")
    for item in outputA:
        print(f"{item['filename']}: {item['standard_term']}")

    print("\n=== 표준화 실패 컬럼명 리스트 ===")
    for item in outputB:
        print(f"{item['filename']}: {item['cleaned_column']}")

    
    identified_columns = defaultdict(list)
    for entry in outputA:
        identified_columns[entry["filename"]].append(entry["original_column"])

    print("3. SBERT 임베딩 기반 유사도 분석 (Module 2)...")
    model = load_sbert_model(MODEL_PATH)
    domain_entries = load_domain_terms()
    input_terms = [item["cleaned_column"] for item in outputB]
    filename_map = {item["cleaned_column"]: item["filename"] for item in outputB}
    original_col_map = {item["cleaned_column"]: item["column"] for item in outputB}

    outputC, outputD = find_similar_terms(input_terms, domain_entries, model, THRESHOLD)
    print(f"  - 유사도 기준 통과: {len(outputC)}개")
    print(f"  - 미달: {len(outputD)}개")
    print("\n[✅ 식별정보로 판단된 컬럼들 (outputC)]")
    for item in outputC:
        print(item)
    print("\n[❌ 식별정보 아님 or 판단 불가한 컬럼들 (outputD)]")
    print("\n=== 식별정보 컬럼 결과 ===")
    for item in outputD:
        print(item)
    print("\n식별정보 컬럼을 파일별로 정리 중...\n")
    # outputC에서 파일명과 원본 컬럼명을 매핑하여 identified_columns에 추가
    print("식별정보 컬럼을 파일별로 정리 중...")
    for item in outputB:
        filename = item["filename"]
        original_col = item["column"]
        if filename not in identified_columns:
            identified_columns[filename] = []
        identified_columns[filename].append(original_col)

    for item in outputC:
        filename = filename_map.get(item["term"], "unknown")
        original_col = original_col_map.get(item["term"], item["term"])
        identified_columns[filename].append(original_col)
    print("\n=== 파일별 식별정보 컬럼 결과 ===")
    for file, cols in identified_columns.items():
        print(f"[{file}] → 식별정보 컬럼: {cols}")
    print("\n2. SBERT 임베딩 기반 유사도 분석 완료\n")

    print("4. 카디널리티 기반 식별정보 판단 (Module 3)...")
    for filename in os.listdir(input_folder):
        file_path = os.path.join(input_folder, filename)
        if filename.endswith('.csv'):
            df = pd.read_csv(file_path)
        elif filename.endswith('.xlsx'):
            df = pd.read_excel(file_path)
        else:
            continue

        id_cols = analyze_dataframe(df)

        for col in id_cols:
            if col not in identified_columns[filename]:
                identified_columns[filename].append(col)
    print(f"  - 식별자로 판단된 컬럼: {len(id_cols)}개")
    print("\n[식별정보로 판단된 컬럼들 (카디널리티 기반)]")
    for col in id_cols:
        print(col)
    print("\n식별정보 컬럼을 파일별로 정리 중...")
    for filename in os.listdir(input_folder):
        file_path = os.path.join(input_folder, filename)
        if filename.endswith('.csv'):
            df = pd.read_csv(file_path)
        elif filename.endswith('.xlsx'):
            df = pd.read_excel(file_path)
        else:
            continue

        id_cols = analyze_dataframe(df)

        for col in id_cols:
            if col not in identified_columns[filename]:
                identified_columns[filename].append(col)
    print("카디널리티 기반 식별정보 판단 완료\n")

    print("\n=== 최종 식별정보 컬럼 결과 ===")
    for file, cols in identified_columns.items():
        print(f"[{file}] → 식별정보 컬럼: {cols}")
    

if __name__ == "__main__":
    main(INPUT_FOLDER)