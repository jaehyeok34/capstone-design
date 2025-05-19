import os
from sentence_transformers import SentenceTransformer, util
from typing import List, Dict, Tuple
from db_utils import get_all_terms_and_stds, insert_new_term_if_high_similarity, connect_db

# SBERT 모델 경로 설정
MODEL_PATH = "model/sbert_domain_model/"
THRESHOLD = 0.84 # 유사도 기준치

# 모델 로드
def load_sbert_model(model_path: str = MODEL_PATH) -> SentenceTransformer:
    return SentenceTransformer(model_path)

# 도메인 사전에서 모든 standard_term 및 메타데이터 불러오기
def load_domain_terms() -> List[Dict]:
    return get_all_terms_and_stds()

# 입력 term들에 대해 SBERT 유사도 측정
def find_similar_terms(
    input_terms: List[str],
    domain_entries: List[Dict],
    model: SentenceTransformer,
    threshold: float = THRESHOLD
) -> Tuple[List[Dict], List[str]]:

    outputC = []  # 유사도 통과
    outputD = []  # 유사도 미달

    input_embeddings = model.encode(input_terms, convert_to_tensor=True)
    std_terms = list({entry["standard_term"] for entry in domain_entries})
    std_embeddings = model.encode(std_terms, convert_to_tensor=True)

    for i, input_term in enumerate(input_terms):
        similarities = util.cos_sim(input_embeddings[i], std_embeddings)[0]
        best_idx = int(similarities.argmax())
        best_score = float(similarities[best_idx])
        best_std_term = std_terms[best_idx]

        if best_score >= threshold:
            # best_std_term에 대응되는 metadata 찾기
            matched_entry = next(
                (entry for entry in domain_entries if entry["standard_term"] == best_std_term),
                None
            )
            if matched_entry:
                result = {
                    "term": input_term,
                    "standard_term": best_std_term,
                    "similarity": best_score,
                    "category": matched_entry["category"],
                    "is_sensitive": matched_entry["is_sensitive"],
                    "language": matched_entry["language"]
                }
                outputC.append(result)

                # 도메인 사전에 없는 term이면 등록
                insert_new_term_if_high_similarity(input_term, matched_entry)
        else:
            outputD.append(input_term)

    return outputC, outputD


if __name__ == "__main__":
    # 예시 입력 (module1의 outputB)
    input_terms = ["주거중인곳", "전화번호", "주거지", "일련번호", "채널번호","본거지","회사주소","집주소","학교명","고향","고향주소","이메일주소","이메일","전화번호"]

    connect_db()

    model = load_sbert_model()
    domain_entries = load_domain_terms()

    outputC, outputD = find_similar_terms(input_terms, domain_entries, model)

    print("[식별정보로 판단된 컬럼들 (outputC)]")
    for item in outputC:
        print(item)

    print("\n[식별정보 아님 or 판단 불가한 컬럼들 (outputD)]")
    print(outputD)