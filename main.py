import os
from collections import defaultdict
import pandas as pd
from domain_search import extract_columns_from_folder, process_columns, clean_column_names
from embedding_search import load_sbert_model, load_domain_terms, find_similar_terms
from cardinality_check import analyze_dataframe

# 기본 경로 설정
INPUT_FOLDER = "data_files" 
MODEL_PATH = "model/sbert_domain_model/"
THRESHOLD = 0.84

def main(input_folder):
    print(" 표준화 및 도메인 사전 검색 ")
    columns_info = extract_columns_from_folder(input_folder)
    print(columns_info)
    outputA, outputB = process_columns(columns_info)

    print(f"추출된 컬럼명: {len(columns_info)}개")
    print("  - 표준화 실패 컬럼:", [item["column"] for item in outputB])

    print("\n=== 원본 컬럼명 리스트 ===")
    for item in columns_info:
        print(f"{item['filename']}: {item['column']}")

    print("\n=== 표준화된 컬럼명 리스트 ===")
    for item in outputA:
        print(f"{item['filename']}: {item['standard_term']}")

    print("\n=== 표준화 실패 컬럼명 리스트 ===")
    for item in outputB:
        print(f"{item['filename']}: {item['cleaned_column']}")

    identified_columns = defaultdict(list)

    # [1] 도메인 사전 기반
    for entry in outputA:
        original_column = entry["original_column"]
        standard_column = entry["standard_term"]
        filename = entry["filename"]
        identified_columns[filename].append(f"{original_column} ({standard_column})")

    # [2] SBERT 기반
    print("\nSBERT 임베딩 기반 유사도 분석")
    model = load_sbert_model(MODEL_PATH)
    domain_entries = load_domain_terms()
    input_terms = [item["cleaned_column"] for item in outputB]
    filename_map = {item["cleaned_column"]: item["filename"] for item in outputB}
    original_col_map = {item["cleaned_column"]: item["column"] for item in outputB}

    outputC, outputD = find_similar_terms(input_terms, domain_entries, model, THRESHOLD)
    print(f"  - 유사도 기준 통과: {len(outputC)}개")
    print(f"  - 미달: {len(outputD)}개")

    print("\n=== 식별정보로 판단된 컬럼들 ===")
    for item in outputC:
        cleaned_col = item["term"]
        standard_term = item["standard_term"]
        filename = filename_map.get(cleaned_col, "unknown")
        original_col = original_col_map.get(cleaned_col, cleaned_col)
        identified_columns[filename].append(f"{original_col} ({standard_term})")
        print(f"{filename}: {original_col} ({standard_term})")

    print("\n=== 식별정보 아님 or 판단 불가한 컬럼들 ===")
    for item in outputD:
        print(item)

    

    # [3] 카디널리티 기반 판단
    print("\n카디널리티 기반 식별정보 판단")

    existing_cleaned_cols = {
        fname: {col.split('(')[-1].replace(')', '').strip() for col in cols if '(' in col and ')' in col}
        for fname, cols in identified_columns.items()
    }
    # 1. outputD를 파일별로 묶음
    columns_for_cardinality = defaultdict(list)
    for term in outputD:
        filename = filename_map.get(term, "unknown")
        original_col = original_col_map.get(term, term)
        columns_for_cardinality[filename].append(original_col)

    # 2. 파일 열고 필요한 컬럼만 판별
    for filename, cols in columns_for_cardinality.items():
        file_path = os.path.join(input_folder, filename)
        if not os.path.exists(file_path):
            continue

        if filename.endswith(".csv"):
            df = pd.read_csv(file_path)
        elif filename.endswith(".xlsx"):
            df = pd.read_excel(file_path)
        else:
            continue

        sub_df = df[cols]  # outputD로 판단된 컬럼만 추출
        card_id_cols = analyze_dataframe(sub_df)

        print(f"\n[{filename}] - 카디널리티 기반 식별정보: {card_id_cols}")

        for col in card_id_cols:
            cleaned_col = clean_column_names([col])[0]
            existing_terms = existing_cleaned_cols.get(filename, set())
            if cleaned_col not in existing_terms:
                identified_columns[filename].append(f"{col} (by_cardinality)")
        # 최종 결과 출력
    print("\n=== 최종 식별정보 컬럼 결과 ===")
    for file, cols in identified_columns.items():
        print(f"[{file}] → 식별정보 컬럼: {cols}")

    

if __name__ == "__main__":
    main(INPUT_FOLDER)