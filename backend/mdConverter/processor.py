import os
import sys
import json
import hashlib
from datetime import datetime
import pandas as pd

# 환경 경로
OUTPUT_DIR = "./output"
JOIN_PROJECTS_DIR = "./join_projects"

os.makedirs(OUTPUT_DIR, exist_ok=True)
os.makedirs(JOIN_PROJECTS_DIR, exist_ok=True)


def _load_projects_from_file() -> list:
    projects_file = os.path.join(JOIN_PROJECTS_DIR, "projects.json")
    if os.path.exists(projects_file):
        try:
            with open(projects_file, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"프로젝트 파일 로드 실패: {e}")
    return []


def _save_projects_to_file(projects: list) -> None:
    projects_file = os.path.join(JOIN_PROJECTS_DIR, "projects.json")
    try:
        with open(projects_file, "w", encoding="utf-8") as f:
            json.dump(projects, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"프로젝트 파일 저장 실패: {e}")


# 기존 main.py에서 사용하던 복잡한 결합키 탐색 로직을 그대로 여기에 둡니다.
# 외부 joinkey 모듈들을 임포트할 수 있게 경로를 설정합니다.
joinkey_path = os.path.join(os.path.dirname(__file__), '..', 'joinkey')
if joinkey_path not in sys.path:
    sys.path.insert(0, joinkey_path)

try:
    from joinkey.analyze_pseudokey import analyze_table
    from joinkey.cardinality_check import analyze_dataframe
    from joinkey.join_key_finder import normalize_colname, standardize_columns
except Exception:
    # 실패 시에는 예외를 무시하고 분석 호출 시 핸들링하도록 합니다.
    analyze_table = None
    analyze_dataframe = None
    normalize_colname = None
    standardize_columns = None


def find_join_keys_for_dataframes(df_a: pd.DataFrame, df_b: pd.DataFrame, 
                                 consistency_threshold: float = 30.0,  
                                 min_unique_ratio: float = 0.3) -> dict:
    # 이 함수는 main.py의 동일한 구현을 재사용합니다.
    # 간결성을 위해 전체 로직을 그대로 복사해 두었습니다.
    try:
        if df_a.empty or df_b.empty:
            return {'error': '빈 데이터프레임', 'join_key_candidates': [], 'dataA_candidates': [], 'dataB_candidates': [], 'total_common_keys': 0, 'recommended_keys': []}

        if standardize_columns is None or analyze_table is None:
            return {'error': 'joinkey 모듈 로드 실패', 'join_key_candidates': [], 'dataA_candidates': [], 'dataB_candidates': [], 'total_common_keys': 0, 'recommended_keys': []}

        df_a_std, mapping_a = standardize_columns(df_a)
        df_b_std, mapping_b = standardize_columns(df_b)

        # DataA 분석
        candidates_a = []
        consistency_results_a = analyze_table(df_a)
        if 'candidates' in consistency_results_a and 'columns' in consistency_results_a:
            for col_name in consistency_results_a['candidates']:
                if col_name in consistency_results_a['columns']:
                    col_info = consistency_results_a['columns'][col_name]
                    final_score = col_info.get('score', 0)
                else:
                    continue

                if final_score >= consistency_threshold:
                    if col_name in df_a.columns:
                        unique_ratio = df_a[col_name].nunique() / len(df_a)
                        is_potential_key = (
                            unique_ratio >= min_unique_ratio or
                            df_a[col_name].nunique() >= 2 or
                            col_name in ['계좌번호', '주민번호', '계좌', '주민등록번호', 'id', 'ID', 'Id']
                        )
                        if is_potential_key:
                            normalized_name = normalize_colname(col_name)
                            candidates_a.append({
                                'original_name': col_name,
                                'normalized_name': normalized_name,
                                'consistency_score': final_score,
                                'unique_ratio': unique_ratio
                            })

        # DataB 분석
        candidates_b = []
        consistency_results_b = analyze_table(df_b)
        if 'candidates' in consistency_results_b and 'columns' in consistency_results_b:
            for col_name in consistency_results_b['candidates']:
                if col_name in consistency_results_b['columns']:
                    col_info = consistency_results_b['columns'][col_name]
                    final_score = col_info.get('score', 0)
                else:
                    continue

                if final_score >= consistency_threshold:
                    if col_name in df_b.columns:
                        unique_ratio = df_b[col_name].nunique() / len(df_b)
                        is_potential_key = (
                            unique_ratio >= min_unique_ratio or
                            df_b[col_name].nunique() >= 2 or
                            col_name in ['계좌번호', '주민번호', '계좌', '주민등록번호', 'id', 'ID', 'Id']
                        )
                        if is_potential_key:
                            normalized_name = normalize_colname(col_name)
                            candidates_b.append({
                                'original_name': col_name,
                                'normalized_name': normalized_name,
                                'consistency_score': final_score,
                                'unique_ratio': unique_ratio
                            })

        # 공통 매칭
        common_keys = []
        for cand_a in candidates_a:
            for cand_b in candidates_b:
                if cand_a['normalized_name'] == cand_b['normalized_name']:
                    sample_values_a = set(str(v) for v in df_a[cand_a['original_name']].dropna().head(100))
                    sample_values_b = set(str(v) for v in df_b[cand_b['original_name']].dropna().head(100))
                    intersection = sample_values_a.intersection(sample_values_b)
                    if len(intersection) > 0 or cand_a['normalized_name'] in ['account', 'resident_id', 'name']:
                        if len(intersection) > 0:
                            similarity_score = len(intersection) / min(len(sample_values_a), len(sample_values_b))
                        else:
                            similarity_score = 0.8 if cand_a['normalized_name'] in ['account', 'resident_id', 'name'] else 0.1
                        is_recommended = (
                            similarity_score > 0.05 or
                            cand_a['normalized_name'] in ['account', 'resident_id', 'name', 'phone', 'gender', 'address']
                        )
                        common_keys.append({
                            'dataA_column': cand_a['original_name'],
                            'dataB_column': cand_b['original_name'],
                            'normalized_name': cand_a['normalized_name'],
                            'dataA_consistency_score': cand_a['consistency_score'],
                            'dataB_consistency_score': cand_b['consistency_score'],
                            'dataA_unique_ratio': cand_a['unique_ratio'],
                            'dataB_unique_ratio': cand_b['unique_ratio'],
                            'value_similarity_score': similarity_score,
                            'recommended': is_recommended
                        })
        common_keys.sort(key=lambda x: (x['recommended'], x['value_similarity_score']), reverse=True)
        recommended_keys = [k for k in common_keys if k['recommended']]

        return {
            'join_key_candidates': common_keys,
            'dataA_candidates': candidates_a,
            'dataB_candidates': candidates_b,
            'total_common_keys': len(common_keys),
            'recommended_keys': recommended_keys
        }
    except Exception as e:
        return {
            'error': f"결합키 분석 중 오류 발생: {str(e)}",
            'join_key_candidates': [],
            'dataA_candidates': [],
            'dataB_candidates': [],
            'total_common_keys': 0,
            'recommended_keys': []
        }


def _compute_hash_series(df: pd.DataFrame, columns: list) -> pd.Series:
    parts = []
    for col in columns:
        if col in df.columns:
            s = df[col].fillna("").astype(str)
        else:
            s = pd.Series([""] * len(df), index=df.index)
        parts.append(s)

    concat = parts[0].astype(str)
    for p in parts[1:]:
        concat = concat.str.cat(p.astype(str))

    return concat.apply(lambda x: hashlib.sha256(x.encode('utf-8')).hexdigest())


def perform_join_for_project(project_id: str) -> str:
    projects = _load_projects_from_file()
    project = next((p for p in projects if str(p.get('id')) == str(project_id)), None)
    if not project:
        raise FileNotFoundError("프로젝트를 찾을 수 없습니다.")

    project_dir = os.path.join(JOIN_PROJECTS_DIR, project_id)
    if not os.path.exists(project_dir):
        raise FileNotFoundError("프로젝트 디렉토리가 존재하지 않습니다.")

    files = project.get('files', [])
    if len(files) < 2:
        raise ValueError("결합하려면 최소 2개 이상의 파일이 필요합니다.")

    file_a = os.path.join(project_dir, files[0])
    file_b = os.path.join(project_dir, files[1])

    if not os.path.exists(file_a) or not os.path.exists(file_b):
        raise FileNotFoundError("입력 파일을 찾을 수 없습니다.")

    def _load_file(path):
        if path.endswith('.csv'):
            return pd.read_csv(path, encoding='utf-8')
        elif path.endswith(('.xlsx', '.xls')):
            return pd.read_excel(path)
        elif path.endswith('.json'):
            return pd.read_json(path)
        else:
            return pd.read_csv(path, encoding='utf-8', errors='ignore')

    df_a = _load_file(file_a)
    df_b = _load_file(file_b)

    join_keys = project.get('joinKeys') or []
    if not join_keys:
        result = find_join_keys_for_dataframes(df_a, df_b)
        candidates = result.get('recommended_keys') or result.get('join_key_candidates', [])
        join_keys = candidates

    cols_a = []
    cols_b = []
    for k in join_keys:
        a_col = k.get('column') or k.get('dataA_column') or k.get('original_name')
        b_col = k.get('matchedColumn') or k.get('dataB_column') or k.get('original_name')
        if a_col and b_col:
            cols_a.append(a_col)
            cols_b.append(b_col)

    if len(cols_a) == 0 or len(cols_b) == 0:
        raise ValueError("사용 가능한 결합키 컬럼이 없습니다.")

    hash_a = _compute_hash_series(df_a, cols_a)
    hash_b = _compute_hash_series(df_b, cols_b)

    df_a['_join_hash_'] = hash_a
    df_b['_join_hash_'] = hash_b

    merged = pd.merge(df_a, df_b, left_on='_join_hash_', right_on='_join_hash_', how='inner', suffixes=('_A', '_B'))

    for a_col, b_col in zip(cols_a, cols_b):
        a_candidates = []
        b_candidates = []
        if a_col in merged.columns:
            a_candidates.append(a_col)
        if f"{a_col}_A" in merged.columns:
            a_candidates.append(f"{a_col}_A")
        if f"{a_col}_B" in merged.columns:
            a_candidates.append(f"{a_col}_B")

        if b_col in merged.columns:
            b_candidates.append(b_col)
        if f"{b_col}_A" in merged.columns:
            b_candidates.append(f"{b_col}_A")
        if f"{b_col}_B" in merged.columns:
            b_candidates.append(f"{b_col}_B")

        canonical_name = a_col
        chosen_a = a_candidates[0] if a_candidates else None
        chosen_b = None
        if b_candidates:
            chosen_b = None
            for c in b_candidates:
                if c.endswith('_B') or c == b_col:
                    chosen_b = c
                    break
            if not chosen_b:
                chosen_b = b_candidates[0]

        if chosen_a:
            if chosen_a != canonical_name:
                merged[canonical_name] = merged[chosen_a]
                if chosen_a.endswith('_A'):
                    merged.drop(columns=[chosen_a], inplace=True)
            if chosen_b and chosen_b in merged.columns:
                merged.drop(columns=[chosen_b], inplace=True)
        else:
            if chosen_b:
                if chosen_b != canonical_name:
                    merged[canonical_name] = merged[chosen_b]
                    if chosen_b.endswith('_B'):
                        merged.drop(columns=[chosen_b], inplace=True)

    if '_join_hash_' in merged.columns:
        merged.drop(columns=['_join_hash_'], inplace=True)

    safe_name = project.get('projectName', 'result').replace(' ', '_')
    result_filename = f"{safe_name}_{project_id[:8]}_결합결과.csv"
    result_path = os.path.join(OUTPUT_DIR, result_filename)
    merged.to_csv(result_path, index=False, encoding='utf-8-sig')

    project['outputFile'] = result_path
    project['status'] = '분석 완료'
    project['progress'] = 100

    for i, p in enumerate(projects):
        if str(p.get('id')) == str(project_id):
            projects[i] = project
            break
    _save_projects_to_file(projects)

    return result_path


def _mask_string(s: str) -> str:
    if s is None:
        return s
    s = str(s)
    if s == "":
        return s
    return s[0] + '*' * (len(s) - 1)


def _numeric_range(val) -> str:
    try:
        if pd.isna(val):
            return val
        iv = int(float(val))
        sign = '-' if iv < 0 else ''
        s = str(abs(iv))
        if len(s) <= 2:
            prefix_len = 1
        else:
            prefix_len = len(s) - 2
        prefix = s[:prefix_len]
        lower = int(prefix + '0' * (len(s) - prefix_len))
        upper = int(prefix + '9' * (len(s) - prefix_len))
        return f"{sign}{lower}-{sign}{upper}"
    except Exception:
        return str(val)


def pseudonymize_project(project_id: str) -> str:
    projects = _load_projects_from_file()
    project = next((p for p in projects if str(p.get('id')) == str(project_id)), None)
    if not project:
        raise FileNotFoundError("프로젝트를 찾을 수 없습니다.")

    result_path = project.get('outputFile')
    if not result_path or not os.path.exists(result_path):
        result_path = perform_join_for_project(project_id)

    df = pd.read_csv(result_path, encoding='utf-8-sig')

    join_keys = project.get('joinKeys') or []
    cols_to_mask = []
    for k in join_keys:
        a_col = k.get('column') or k.get('dataA_column') or k.get('original_name')
        b_col = k.get('matchedColumn') or k.get('dataB_column') or k.get('original_name')
        if a_col:
            cols_to_mask.append(a_col)
        if b_col:
            cols_to_mask.append(b_col)

    cols_to_mask = [c for c in dict.fromkeys(cols_to_mask) if c]

    if not cols_to_mask:
        for col in df.columns:
            if df[col].dtype.kind in ('O', 'S', 'U') or pd.api.types.is_numeric_dtype(df[col]):
                cols_to_mask.append(col)

    df_pseudo = df.copy()
    for col in cols_to_mask:
        if col not in df_pseudo.columns:
            continue
        if pd.api.types.is_numeric_dtype(df_pseudo[col]):
            df_pseudo[col] = df_pseudo[col].apply(_numeric_range)
        else:
            df_pseudo[col] = df_pseudo[col].fillna('').astype(str).apply(_mask_string)

    pseudo_filename = os.path.splitext(os.path.basename(result_path))[0] + '_pseudonymized.csv'
    pseudo_path = os.path.join(OUTPUT_DIR, pseudo_filename)
    df_pseudo.to_csv(pseudo_path, index=False, encoding='utf-8-sig')

    project['pseudonymizedFile'] = pseudo_path
    _save_projects_to_file(projects)

    return pseudo_path
