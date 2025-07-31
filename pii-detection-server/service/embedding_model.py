from typing import List, Tuple
from flask import current_app
from sentence_transformers import SentenceTransformer, util
from service.db_utils import get_domain, insert_new_term


def embedding_model(columns: List[Tuple[str, str]]):
    pii, non_pii = [], []
    model = __load_sbert_model(current_app.config['SBERT_MODEL_PATH'])

    domain = get_domain()
    
    normalized = [normalized for _, normalized in columns]
    standard_terms = list({x['standard_term'] for x in domain})

    input_embeddings = model.encode(normalized, convert_to_tensor=True)
    std_embeddings = model.encode(standard_terms, convert_to_tensor=True)

    for i, input in enumerate(input_embeddings):
        similarities = util.cos_sim(input, std_embeddings)[0] # cos_sim이 이중 배열로 반환함 [[결과]]
        best_idx = int(similarities.argmax())
        best_socre = float(similarities[best_idx])
        best_std_term = standard_terms[best_idx]

        threshold = float(current_app.config['THRESHOLD'])
        if best_socre < threshold:
            non_pii.append(columns[i])
            continue

        matched_domain = next((x for x in domain if x['standard_term'] == best_std_term))
        new_term = normalized[i]
        
        pii.append(columns[i])  
        insert_new_term(new_term, matched_domain)

    return pii, non_pii


def __load_sbert_model(model_path: str) -> SentenceTransformer:
    """SBERT 모델 로드"""
    return SentenceTransformer(model_path)