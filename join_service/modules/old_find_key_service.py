import base64
from concurrent.futures import ThreadPoolExecutor, as_completed
import json
from typing import Dict, List, Tuple
from threading import Event

from modules.analyze_pseudokey import analyze_table
from modules.join_key_finder import normalize_colname, standardize_columns
from py_client.agent import Agent
from py_client.message.message import Message
from utils import to_dataframe

FIND = 1

def run_find_key(agent: Agent, topic_name: str, executor: ThreadPoolExecutor, stop: Event):
    def handler(message: Message):
        print("! find 시작")
        try:
            json_files = json.loads(message.payload.decode("utf-8"))
            files = [base64.b64decode(f) for f in json_files]
            join_key = find_keys((files[0], files[1]))
            print("! find: 조인키 탐색 결과:", "error" not in join_key)
            agent.producer.asyncProduce(
                topic_name=topic_name, partition=str(-FIND),
                header=message.header, payload=json.dumps(join_key, ensure_ascii=False)
            )
        except Exception as e:
            header = message.header | {"error": str(e)}
            agent.producer.asyncProduce(topic_name=topic_name, partition=str(-FIND), header=header)
        
        print("! find 종료")

    while not stop.is_set():
        consumed = agent.consumer.consume(topic_name=topic_name, partition=str(FIND))[0]
        if consumed.get_header("ok", "false").lower() != "true" and not consumed.payload:
            print("! run_find(): 유효하지 않은 메시지 수신")
            continue

        executor.submit(handler, consumed)
            

def find_keys(file_content_pair: Tuple[bytes, bytes]):
    print("! find_keys(): 조인키 탐색 시작")
    df_a, df_b = (to_dataframe(file_content) for file_content in file_content_pair)
    if (df_a is None or df_a.empty) or (df_b is None or df_b.empty):
        return { "error": "dataframe이 생성되지 않았거나 비어 있음" }
    
    consistency_threshold = 30.0
    min_unique_ratio = 0.3
    
    # dataA, dataB 분석
    with ThreadPoolExecutor(max_workers=15) as executor:
        future_a = executor.submit(standardize_columns, df_a)
        future_b = executor.submit(standardize_columns, df_b)

        df_a_std, mapping_a = future_a.result()
        future_a = executor.submit(condidates, analyze_table(df_a), df_a, consistency_threshold, min_unique_ratio)

        df_b_std, mapping_b = future_b.result()
        future_b = executor.submit(condidates, analyze_table(df_b), df_b, consistency_threshold, min_unique_ratio)

        candidates_a = future_a.result()
        candidates_b = future_b.result()

    if (candidates_a is None) or (candidates_b is None):
        return { "error": "일관성 분석 중 오류 발생" }

    common_keys: List[Dict] = []
    pairs = [(cand_a, cand_b, df_a, df_b) for cand_a in candidates_a for cand_b in candidates_b]
    with ThreadPoolExecutor(max_workers=15) as executor:
        futures = [executor.submit(compare_candidates, pair) for pair in pairs]
        for f in as_completed(futures):
            result = f.result()
            if result is not None:
                common_keys.append(result)

    # 결과
    common_keys.sort(key=lambda x: (x['recommended'], x['value_similarity_score']), reverse=True)
    recommended_keys = [k for k in common_keys if k['recommended']]

    print("! find_keys(): 조인키 탐색 완료")
    return {
        'join_key_candidates': common_keys,
        'dataA_candidates': candidates_a,
        'dataB_candidates': candidates_b,
        'total_common_keys': len(common_keys),
        'recommended_keys': recommended_keys
    }

def compare_candidates(pair):
    cand_a, cand_b, df_a, df_b = pair
    if cand_a['normalized_name'] != cand_b['normalized_name']:
        return None

    sample_values_a = set(str(v) for v in df_a[cand_a['original_name']].dropna().head(100))
    sample_values_b = set(str(v) for v in df_b[cand_b['original_name']].dropna().head(100))
    intersection = sample_values_a.intersection(sample_values_b)
    if (len(intersection) <= 0) and (cand_a['normalized_name'] not in ['account', 'resident_id', 'name']):
        return None

    if len(intersection) > 0:
        similarity_score = len(intersection) / min(len(sample_values_a), len(sample_values_b))
    else:
        similarity_score = 0.8 if cand_a['normalized_name'] in ['account', 'resident_id', 'name'] else 0.1

    is_recommended = (
        similarity_score > 0.05 or
        cand_a['normalized_name'] in ['account', 'resident_id', 'name', 'phone', 'gender', 'address']
    )

    return {
        'dataA_column': cand_a['original_name'],
        'dataB_column': cand_b['original_name'],
        'normalized_name': cand_a['normalized_name'],
        'dataA_consistency_score': cand_a['consistency_score'],
        'dataB_consistency_score': cand_b['consistency_score'],
        'dataA_unique_ratio': cand_a['unique_ratio'],
        'dataB_unique_ratio': cand_b['unique_ratio'],
        'value_similarity_score': similarity_score,
        'recommended': is_recommended
    }

def condidates(consistency_result, df, consistency_threshold, min_unique_ratio):
    candidates = []

    if ('candidates' not in consistency_result) or ('columns' not in consistency_result):
        return None
    
    for col_name in consistency_result['candidates']:
        if (not col_name) or (col_name not in df.columns):
            continue

        col_info = consistency_result['columns'][col_name]
        final_score = col_info.get('score', 0)
        if (final_score < consistency_threshold) or (col_name not in df.columns):
            continue

        unique_ratio = df[col_name].nunique() / len(df)
        is_potential_key = (
            unique_ratio >= min_unique_ratio or
            df[col_name].nunique() >= 2 or
            col_name in ['계좌번호', '주민번호', '계좌', '주민등록번호', 'id', 'ID', 'Id']
        )
        if not is_potential_key:
            continue

        normalized_name = normalize_colname(col_name)
        candidates.append({
            'original_name': col_name,
            'normalized_name': normalized_name,
            'consistency_score': final_score,
            'unique_ratio': unique_ratio
        })
    
    return candidates