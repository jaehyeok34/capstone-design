import os
from sentence_transformers import SentenceTransformer, util
from typing import List, Dict, Tuple
from db_utils import get_all_terms_and_stds, insert_new_term_if_high_similarity, connect_db

# SBERT 모델 경로 및 유사도 기준치 설정
MODEL_PATH = "model/sbert_domain_model/"
THRESHOLD = 0.84  # 유사도 기준치 0.84

def load_sbert_model(model_path: str = MODEL_PATH) -> SentenceTransformer:
    """SBERT 모델 로드"""
    return SentenceTransformer(model_path)

def load_domain_terms() -> List[Dict]:
    """DB에서 모든 도메인 사전 항목 불러오기"""
    return get_all_terms_and_stds()

def find_similar_terms(
    input_terms: List[str],
    domain_entries: List[Dict],
    model: SentenceTransformer,
    threshold: float = THRESHOLD
) -> Tuple[List[Dict], List[str]]:
    """
    입력된 term에 대해 도메인 사전과의 유사도를 측정하여
    기준치 이상인 경우 outputC, 미달인 경우 outputD에 분류
    """

    outputC = []  # 유사도 통과 항목
    outputD = []  # 미통과 항목

    # 입력 및 표준 용어 임베딩
    input_embeddings = model.encode(input_terms, convert_to_tensor=True)
    std_terms = list({entry["standard_term"] for entry in domain_entries})
    std_embeddings = model.encode(std_terms, convert_to_tensor=True)

    for i, input_term in enumerate(input_terms):
        similarities = util.cos_sim(input_embeddings[i], std_embeddings)[0]
        best_idx = int(similarities.argmax())
        best_score = float(similarities[best_idx])
        best_std_term = std_terms[best_idx]

        if best_score >= threshold:
            # 유사한 standard_term에 대응되는 메타데이터 검색
            matched_entry = next(
                (entry for entry in domain_entries if entry["standard_term"] == best_std_term),
                None
            )
            if matched_entry:
                result = {
                    "term": input_term,
                    "standard_term": best_std_term,
                    "similarity": round(best_score, 4),
                    "category": matched_entry.get("category", "N/A"),
                    "is_sensitive": matched_entry.get("is_sensitive", 0),
                    "synonym_group_id": matched_entry.get("synonym_group_id", -1)
                }
                outputC.append(result)

                # DB에 자동 삽입 (중복 무시)
                insert_new_term_if_high_similarity(input_term, matched_entry)
        else:
            outputD.append(input_term)

    return outputC, outputD


if __name__ == "__main__":
    # 예시 입력 (module1의 outputB에서 온 컬럼들)
    input_terms = [
        "주거중인곳", "전화번호", "주거지", "일련번호", "채널번호",
        "본거지", "회사주소", "집주소", "학교명", "고향", 
        "고향주소", "이메일주소", "이메일", "전화번호" ,"서식지"
    ]

    # DB 접속 및 모델 로딩
    connect_db()
    model = load_sbert_model()
    domain_entries = load_domain_terms()

    # 유사도 판단 실행
    outputC, outputD = find_similar_terms(input_terms, domain_entries, model)

    # 결과 출력
    print("[✅ 식별정보로 판단된 컬럼들 (outputC)]")
    for item in outputC:
        print(item)

    print("\n[❌ 식별정보 아님 or 판단 불가한 컬럼들 (outputD)]")
    print(outputD)