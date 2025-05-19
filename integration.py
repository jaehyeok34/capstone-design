# 
from domain_search import extract_columns_from_folder, process_columns
from embedding_search import find_similar_terms, load_sbert_model, load_domain_terms
from cardinality_check import analyze_dataframe
from db_utils import insert_new_term_if_high_similarity

from typing import List, Dict
import os
import pandas as pd

# 경로 설정
DATA_FOLDER = "data_files"
OUTPUT_FOLDER = "output"
os.makedirs(OUTPUT_FOLDER, exist_ok=True)

# 1단계: 파일에서 컬럼 추출
columns_info = extract_columns_from_folder(DATA_FOLDER)
print("1단계: 컬럼 추출 완료")

# 2단계: 도메인 사전 기반 처리
outputA, outputB = process_columns(columns_info)
print("2단계: 도메인 사전 기반 표준화 완료")
print(f"  - 표준화 성공: {len(outputA)}개")
print(outputA)
print(f"  - 표준화 실패: {len(outputB)}개")
print(outputB)

# 3단계: SBERT 유사도 기반 유사한 term 탐색
model = load_sbert_model()
domain_entries = load_domain_terms()
input_terms = list(set([item["cleaned_column"] for item in outputB]))  # 중복 제거

outputC, outputD = find_similar_terms(input_terms, domain_entries, model)
print("3단계: SBERT 유사도 분석 완료")
print(f"  - 유사도 기준 통과: {len(outputC)}개")
print(outputC)
print(f"  - 미달: {len(outputD)}개")
print(outputD)

# 4단계: SBERT로도 판단 안 된 컬럼들 → 원래 어떤 파일에 있었는지 확인 → 해당 파일 열기
potential_identifier_columns = []
for filename in os.listdir(DATA_FOLDER):
    if not filename.endswith(('.csv', '.xlsx')):
        continue

    file_path = os.path.join(DATA_FOLDER, filename)
    try:
        if filename.endswith('.csv'):
            df = pd.read_csv(file_path)
        elif filename.endswith('.xlsx'):
            df = pd.read_excel(file_path)
    except Exception as e:
        print(f"[오류] {filename} 파일 로딩 실패: {e}")
        continue

    # outputD에서 해당 파일의 컬럼만 필터링
    related_cols = [item["column"] for item in outputB if item["filename"] == filename and item["cleaned_column"] in outputD]
    if not related_cols:
        continue

    subset_df = df[related_cols]
    identified = analyze_dataframe(subset_df, min_ratio=0.95)
    for col in identified:
        potential_identifier_columns.append({"filename": filename, "column": col})

print("4단계: 카디널리티 기반 분석 완료")
print(f"  - 식별자로 판단된 컬럼: {len(potential_identifier_columns)}개")
print(potential_identifier_columns)

# 요약 출력
print("\n[식별정보 판단 요약]")
print(f"1) 도메인 사전 기반 식별: {len(outputA)}")
print(f"2) SBERT 유사도 기반 식별: {len(outputC)}")
print(f"3) 고유값 비율 기반 식별: {len(potential_identifier_columns)}")

# 필요 시 출력 파일로 저장
pd.DataFrame(outputA).to_csv(os.path.join(OUTPUT_FOLDER, "outputA_domain_match.csv"), index=False)
pd.DataFrame(outputC).to_csv(os.path.join(OUTPUT_FOLDER, "outputC_sbert_match.csv"), index=False)
pd.DataFrame(potential_identifier_columns).to_csv(os.path.join(OUTPUT_FOLDER, "outputE_cardinality.csv"), index=False)

import pandas as pd
from collections import defaultdict
from itertools import combinations

# standard_term별로 파일들을 모읍니다
term_to_files = defaultdict(set)

for row in outputA:
    filename = row["filename"]
    standard_term = row["standard_term"]
    term_to_files[standard_term].add(filename)

for row in outputC:
    filename = row["filename"]
    standard_term = row["standard_term"]
    term_to_files[standard_term].add(filename)

# 결과 리스트
newList = []

for term, files in term_to_files.items():
    if len(files) > 1:
        for f1, f2 in combinations(sorted(files), 2):
            newList.append((term, f1, f2))

# 출력
print("\n✅ 서로 다른 파일에서 동일한 standard_term이 존재하는 항목들:")
for item in newList:
    print(item)