#!/usr/bin/env python3
"""
join_key_finder.py

여러 데이터셋(파일/폴더)을 입력받아 다음 파이프라인으로 결합키 후보를 자동 발굴합니다.
1) 컬럼명 표준화(정규화)
2) analyze_pseudokey 기반 일관성 점수 필터
3) cardinality_check 기반 고유성(기수 비율) 필터
4) 모든(혹은 최소 k개) 데이터셋에서 공통으로 통과한 컬럼을 결합키 후보로 제안

사용법 예시:
  python join_key_finder.py data_files --min-unique-ratio 0.95 --consistency-threshold 70 --require-all
  python join_key_finder.py data_files/input_A.csv data_files/datas.csv --require-at-least 2
"""

import os
import sys
import re
import json
from typing import Dict, Any, List, Tuple, Set, Optional

import pandas as pd

from modules.analyze_pseudokey import (
    safe_load_file,
    analyze_table,
    normalize_value,
    DEFAULT_CONFIG,
)
from modules.cardinality_check import is_potential_identifier


# --------------------------- Column name normalization ---------------------------

# 컬럼명 표준화 매핑 사전
COLUMN_STANDARDIZATION_MAP = {
    # 계좌 관련
    "계좌": "account",
    "계좌번호": "account", 
    "계좌_번호": "account",
    "account_number": "account",
    "account_no": "account",
    "acct_no": "account",
    "acct": "account",
    
    # 주민번호 관련
    "주민번호": "resident_id",
    "주민등록번호": "resident_id",
    "주민_번호": "resident_id",
    "주민등록_번호": "resident_id",
    "resident_registration_number": "resident_id",
    "ssn": "resident_id",
    "national_id": "resident_id",
    
    # 이름 관련
    "이름": "name",
    "성명": "name",
    "성함": "name",
    "full_name": "name",
    "user_name": "name",
    "customer_name": "name",
    
    # 성별 관련
    "성별": "gender",
    "sex": "gender",
    "gender": "gender",
    
    # 전화번호 관련
    "전화번호": "phone",
    "휴대전화번호": "phone",
    "휴대폰번호": "phone",
    "핸드폰번호": "phone",
    "phone": "phone",
    "phone_number": "phone",
    "mobile": "phone",
    "cell_phone": "phone",
    "tel": "phone",
    
    # 주소 관련
    "주소": "address",
    "거주지": "address",
    "서식지": "address",
    "주거지": "address",
    "address": "address",
    "residence": "address",
    "location": "address",
    
    # 생년월일 관련
    "생년월일": "birth_date",
    "생일": "birth_date",
    "출생일": "birth_date",
    "birth_date": "birth_date",
    "birthday": "birth_date",
    "date_of_birth": "birth_date",
    "dob": "birth_date",
    
    # 나이 관련
    "나이": "age",
    "연령": "age",
    "age": "age",
    
    # ID 관련
    "id": "id",
    "아이디": "id",
    "고객id": "customer_id",
    "사용자id": "user_id",
    "user_id": "user_id",
    "customer_id": "customer_id",
    
    # 이메일 관련
    "이메일": "email",
    "메일": "email",
    "email": "email",
    "e_mail": "email",
}

def normalize_colname(name: str) -> str:
    """
    컬럼명을 의미 기반으로 표준화
    1. 기본 정규화 (소문자, 구분자 통일)
    2. 의미적 표준화 (동일한 의미의 컬럼명을 표준명으로 통일)
    """
    if name is None:
        return ""
    
    # 1단계: 기본 정규화
    s = str(name).strip().lower()
    # 공백류 포함 구분자들을 '_'로 치환
    s = re.sub(r"[\s\-\.,/\\|:;]+", "_", s)
    # 연속 '_'는 1개로 축소
    s = re.sub(r"_+", "_", s)
    s = s.strip("_")
    
    # 2단계: 의미적 표준화
    # 정확한 매칭 먼저 시도
    if s in COLUMN_STANDARDIZATION_MAP:
        return COLUMN_STANDARDIZATION_MAP[s]
    
    # 부분 매칭 시도 (키워드 포함)
    for key, standard_name in COLUMN_STANDARDIZATION_MAP.items():
        if key in s or s in key:
            # 더 구체적인 매칭을 위해 길이도 고려
            if abs(len(key) - len(s)) <= 3:  # 길이 차이가 3자 이하인 경우만
                return standard_name
    
    # 매칭되지 않으면 기본 정규화된 이름 반환
    return s


def standardize_columns(df: pd.DataFrame) -> Tuple[pd.DataFrame, Dict[str, str]]:
    """DataFrame의 컬럼명을 표준화해서 반환.
    returns: (표준화된 df, {원래컬럼명: 표준컬럼명})
    """
    mapping = {col: normalize_colname(col) for col in df.columns}
    # 표준명 충돌 방지: 중복시 접미사 부여
    seen = {}
    for orig, norm in list(mapping.items()):
        if norm == "":
            norm = "col"
        if norm in seen:
            seen[norm] += 1
            new_norm = f"{norm}_{seen[norm]}"
            mapping[orig] = new_norm
            seen[new_norm] = 1
        else:
            seen[norm] = 1
    std_df = df.copy()
    std_df.columns = [mapping[c] for c in df.columns]
    return std_df, mapping


# --------------------------- Values normalization for uniqueness ---------------------------
def normalize_values_df(df: pd.DataFrame) -> pd.DataFrame:
    """값 정규화: 공백/구분자 제거 + 소문자화 등(분석 스크립트의 normalize_value 재사용)"""
    out = pd.DataFrame(index=df.index)
    for col in df.columns:
        out[col] = df[col].astype(str).map(normalize_value)
    return out


# --------------------------- Alias (synonym) mapping for column names ---------------------------
def load_alias_map(path: str) -> Dict[str, List[str]]:
    """동의어 매핑 JSON 로드. 형식 예시:
    {
      "ssn": ["주민번호", "주민_번호"],
      "phone_number": ["폰번호", "전화번호"],
      "email": ["이메일"]
    }
    키는 '정규화된 표준 컬럼명'이어야 하며 값 리스트의 각 항목도 normalize_colname으로 정규화되어 비교됩니다.
    """
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    # 모두 표준화
    norm_map: Dict[str, List[str]] = {}
    for canon, aliases in data.items():
        c = normalize_colname(canon)
        norm_map[c] = sorted(list({normalize_colname(a) for a in (aliases or [])}))
    return norm_map


def apply_alias_to_columns(df: pd.DataFrame, alias_map: Dict[str, List[str]]) -> Tuple[pd.DataFrame, Dict[str, str]]:
    """표준화된 컬럼명 df에 동의어 매핑 적용하여 '정해진 표준명(canonical)'으로 컬럼명을 통합.
    반환: (컬럼명 변경된 df, {원래표준명: 최종표준명})
    주의: 입력 df의 컬럼은 이미 standardize_columns가 적용된 상태라고 가정.
    """
    if not alias_map:
        return df, {c: c for c in df.columns}

    # 역인덱스: alias -> canonical
    reverse: Dict[str, str] = {}
    for canon, aliases in alias_map.items():
        reverse[canon] = canon
        for a in aliases:
            reverse[a] = canon

    rename_map: Dict[str, str] = {}
    for c in df.columns:
        nc = reverse.get(c, c)
        rename_map[c] = nc

    out = df.rename(columns=rename_map).copy()
    return out, rename_map


# --------------------------- Candidate extraction per dataset ---------------------------
def candidates_for_dataset(
    name: str,
    df: pd.DataFrame,
    consistency_threshold: float,
    min_unique_ratio: float,
    alias_map: Optional[Dict[str, List[str]]] = None,
) -> Dict[str, Any]:
    """하나의 테이블(데이터셋)에 대해
    - 일관성 통과 컬럼(분석 점수 기준)
    - 고유성 통과 컬럼(정규화 값 기준 nunique/rows >= min_unique_ratio)
    - 둘 다 통과 컬럼
    을 계산.
    """
    std_df, colmap = standardize_columns(df)
    # 동의어 매핑 적용(표준화된 이름 기반)
    std_df, alias_rename = apply_alias_to_columns(std_df, alias_map or {})

    # 1) 일관성 점수
    table_res = analyze_table(std_df, config=DEFAULT_CONFIG)
    score_by_col = {c: table_res["columns"][c]["score"] for c in std_df.columns}
    consistency_pass = {c for c, sc in score_by_col.items() if sc >= consistency_threshold}

    # 2) 고유성 (정규화된 값 기준으로 더 보수적으로 판단)
    norm_df = normalize_values_df(std_df)
    unique_pass: Set[str] = set()
    nrows = len(norm_df)
    for c in norm_df.columns:
        series = norm_df[c]
        # cardinality_check의 판정 함수를 사용 (정확히는 nunique/rows 비교)
        # NaN은 문자열화되어 들어왔으므로 dropna는 생략 가능하지만 안전하게 유지
        if nrows > 0:
            # 직접 비율 계산
            ratio = series.dropna().nunique() / nrows
            if ratio >= min_unique_ratio:
                unique_pass.add(c)
            else:
                # is_potential_identifier도 한 번 더 확인(동일 로직이지만 향후 내부 로직 변경 대비)
                if is_potential_identifier(series, min_ratio=min_unique_ratio):
                    unique_pass.add(c)

    both_pass = sorted(list(consistency_pass & unique_pass))

    return {
        "name": name,
        "nrows": len(std_df),
        "columns_map": colmap,  # {원본:표준}
        "alias_applied": alias_rename,  # {표준:최종표준(동의어 통합후)}
        "consistency_pass": sorted(list(consistency_pass)),
        "unique_pass": sorted(list(unique_pass)),
        "both_pass": both_pass,
        "score_by_column": score_by_col,
    }


# --------------------------- Global intersection ---------------------------
def global_candidates(
    dataset_results: List[Dict[str, Any]],
    require_all: bool = True,
    require_at_least: int = 2,
) -> Dict[str, Any]:
    """여러 데이터셋의 both_pass 컬럼을 교집합으로 취합.
    - require_all=True: 모든 데이터셋에 공통으로 존재하는 컬럼만
    - require_all=False and require_at_least=k: 최소 k개 이상에서 공통으로 등장하는 컬럼들
    반환: {intersection, coverage_by_dataset}
    """
    sets = [set(r["both_pass"]) for r in dataset_results]
    if not sets:
        return {"intersection": [], "coverage_by_dataset": {}}

    if require_all:
        inter = set.intersection(*sets) if len(sets) > 1 else sets[0]
    else:
        counts = {}
        for s in sets:
            for c in s:
                counts[c] = counts.get(c, 0) + 1
        inter = {c for c, cnt in counts.items() if cnt >= max(1, require_at_least)}

    coverage = {}
    for r in dataset_results:
        name = r["name"]
        present = sorted(list(set(r["both_pass"]) & inter))
        coverage[name] = present

    return {"intersection": sorted(list(inter)), "coverage_by_dataset": coverage}


# --------------------------- Load all inputs ---------------------------
def load_all_tables(paths: List[str]) -> List[Tuple[str, pd.DataFrame]]:
    """여러 파일/폴더 경로에서 테이블을 로드하여 (이름, DataFrame) 리스트 반환."""
    tables: List[Tuple[str, pd.DataFrame]] = []
    for p in paths:
        t = safe_load_file(p)
        tables.extend(t)
    return tables


# --------------------------- CLI ---------------------------
def parse_args(argv: List[str]) -> Dict[str, Any]:
    import argparse
    parser = argparse.ArgumentParser(description="자동 결합키 후보 탐색기")
    parser.add_argument("paths", nargs="+", help="분석할 파일/폴더 경로(복수 가능)")
    parser.add_argument("--min-unique-ratio", type=float, default=0.95, help="고유성 최소 비율 기준 (기본 0.95)")
    parser.add_argument("--consistency-threshold", type=float, default=DEFAULT_CONFIG.get("score_threshold", 70), help="일관성 점수 기준 (기본 DEFAULT_CONFIG.score_threshold)")
    parser.add_argument("--alias-map", type=str, default=None, help="컬럼명 동의어 매핑 JSON 파일 경로")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--require-all", action="store_true", help="모든 데이터셋에 공통으로 존재하는 컬럼만 선택")
    mode.add_argument("--require-at-least", type=int, default=None, help="최소 N개 데이터셋에서 공통으로 등장하는 컬럼 선택")
    parser.add_argument("--print-details", action="store_true", help="데이터셋별 상세 결과 포함 출력")
    args = parser.parse_args(argv[1:])

    require_all = True
    at_least = 2
    if args.require_at_least is not None:
        require_all = False
        at_least = max(1, int(args.require_at_least))
    elif args.require_all:
        require_all = True
    else:
        # 기본은 require_all
        require_all = True

    return {
        "paths": args.paths,
        "min_unique_ratio": args.min_unique_ratio,
        "consistency_threshold": args.consistency_threshold,
        "alias_map_path": args.alias_map,
        "require_all": require_all,
        "require_at_least": at_least,
        "print_details": args.print_details,
    }


def cli_main(argv: List[str]) -> int:
    opts = parse_args(argv)

    tables = load_all_tables(opts["paths"])
    if not tables:
        print(json.dumps({
            "error": "no supported files/tables found at given paths",
            "paths": opts["paths"],
        }, ensure_ascii=False, indent=2))
        return 1

    alias_map: Dict[str, List[str]] = {}
    if opts.get("alias_map_path"):
        try:
            alias_map = load_alias_map(opts["alias_map_path"])
        except Exception as e:
            print(json.dumps({
                "warning": f"failed to load alias map: {e}",
                "alias_map_path": opts["alias_map_path"],
            }, ensure_ascii=False))
            alias_map = {}

    dataset_results: List[Dict[str, Any]] = []
    for name, df in tables:
        try:
            res = candidates_for_dataset(
                name=name,
                df=df,
                consistency_threshold=opts["consistency_threshold"],
                min_unique_ratio=opts["min_unique_ratio"],
                alias_map=alias_map,
            )
            dataset_results.append(res)
        except Exception as e:
            dataset_results.append({
                "name": name,
                "error": str(e),
            })

    global_res = global_candidates(
        dataset_results,
        require_all=opts["require_all"],
        require_at_least=opts["require_at_least"],
    )

    out: Dict[str, Any] = {
        "config": {
            "min_unique_ratio": opts["min_unique_ratio"],
            "consistency_threshold": opts["consistency_threshold"],
            "require": "all" if opts["require_all"] else "at_least",
            "k": opts["require_at_least"] if not opts["require_all"] else None,
        },
        "global_candidates": global_res,
    }

    if opts["print_details"]:
        out["datasets"] = {
            r["name"]: {
                k: r.get(k)
                for k in [
                    "nrows",
                    "columns_map",
                    "alias_applied",
                    "consistency_pass",
                    "unique_pass",
                    "both_pass",
                    "score_by_column",
                    "error",
                ]
                if k in r
            }
            for r in dataset_results
        }

    print(json.dumps(out, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(cli_main(sys.argv))
