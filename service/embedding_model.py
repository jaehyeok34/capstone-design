from typing import List, Tuple
from flask import current_app
from sentence_transformers import SentenceTransformer


def __load_sbert_model(model_path: str) -> SentenceTransformer:
    """SBERT 모델 로드"""
    return SentenceTransformer(model_path)


def embedding_model(columns: List[Tuple[str, str]]):
    model = __load_sbert_model(current_app.config['SBERT_MODEL_PATH'])

    pass

# def find_similar_terms(
#     input_terms: List[str],
#     domain_entries: List[Dict],
#     model: SentenceTransformer,
#     threshold: float = THRESHOLD
# ) -> Tuple[List[Dict], List[str]]:
#     """
#     입력된 term에 대해 도메인 사전과의 유사도를 측정하여
#     기준치 이상인 경우 outputC, 미달인 경우 outputD에 분류
#     """

#     outputC = []  # 유사도 통과 항목
#     outputD = []  # 미통과 항목

#     # 입력 및 표준 용어 임베딩
#     input_embeddings = model.encode(input_terms, convert_to_tensor=True)
#     std_terms = list({entry["standard_term"] for entry in domain_entries})
#     std_embeddings = model.encode(std_terms, convert_to_tensor=True)

#     for i, input_term in enumerate(input_terms):
#         similarities = util.cos_sim(input_embeddings[i], std_embeddings)[0]
#         best_idx = int(similarities.argmax())
#         best_score = float(similarities[best_idx])
#         best_std_term = std_terms[best_idx]

#         if best_score >= threshold:
#             # 유사한 standard_term에 대응되는 메타데이터 검색
#             matched_entry = next(
#                 (entry for entry in domain_entries if entry["standard_term"] == best_std_term),
#                 None
#             )
#             if matched_entry:
#                 result = {
#                     "term": input_term,
#                     "standard_term": best_std_term,
#                     "similarity": round(best_score, 4),
#                     "category": matched_entry.get("category", "N/A"),
#                     "is_sensitive": matched_entry.get("is_sensitive", 0),
#                     "synonym_group_id": matched_entry.get("synonym_group_id", -1)
#                 }
#                 outputC.append(result)

#                 insert_new_term_if_high_similarity(input_term, matched_entry)
#         else:
#             outputD.append(input_term)

#     return outputC, outputD
