#!/usr/bin/env python3
# analyze_pseudokey.py
# Rule-based consistency checks to propose candidate join-key columns from files or directories.
# Supports CSV, Excel (.xls/.xlsx), JSON.
# Outputs a JSON-like dict with candidate columns and per-column diagnostics.
#
# Usage:
#   python analyze_pseudokey.py /path/to/file_or_directory
# or, import functions from this file in another script:
#   from analyze_pseudokey import analyze_path, analyze_table
#
# NOTE: Thresholds and weights are defaults and should be tuned per-domain. (추측입니다)

from concurrent.futures import ThreadPoolExecutor, as_completed
import os
import sys
import json
import math
import re
from collections import Counter
from typing import Dict, Any, List, Tuple

try:
    import pandas as pd
    import numpy as np
except Exception as e:
    raise RuntimeError("This script requires pandas and numpy installed.") from e

# --------------------------- Configuration (tunable) ---------------------------
DEFAULT_CONFIG = {
    "weights": {
        "type_consistency": 15,
        "format_match": 20,
        "length_consistency": 10,
        "missing_rate": 15,
        "unique_ratio": 20,
        "entropy": 10,
        "stable_over_norm": 10
    },
    # Thresholds used to decide if a single rule is "satisfied". These are guesses.
    "thresholds": {
        "type_consistency_ratio": 0.95,
        "format_match_ratio": 0.9,
        "length_cv": 0.2,
        "max_missing_rate": 0.1,
        "min_unique_ratio": 0.5,
        "min_entropy": 1.5,
        "stable_norm_ratio": 0.9
    },
    "formats": {
        "email": r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$",
        "phone_international": r"^\+?\d{1,3}[-\s]?\(?\d{1,4}\)?[-\s]?\d{3,4}[-\s]?\d{3,4}$",
        "digits_6_14": r"^\d{6,14}$",
        "id_like": r"^\d{6,}[-\s]?\d+$",
        "alpha_num_hyphen": r"^[A-Za-z0-9\-]+$",
        "zip_korea": r"^\d{3}-?\d{3}$"
    },
    "score_threshold": 70  # total score (0-100) to be considered candidate (추측입니다)
}

# --------------------------- Utility functions ---------------------------
def safe_load_file(path: str) -> List[Tuple[str, pd.DataFrame]]:
    """
    Load a file or all files in a directory (supported: csv, xlsx/xls, json)
    Returns list of (name, dataframe)
    """
    tables = []
    if os.path.isdir(path):
        for fname in os.listdir(path):
            full = os.path.join(path, fname)
            if os.path.isfile(full):
                try:
                    tables.extend(safe_load_file(full))
                except Exception:
                    # ignore unreadable files
                    continue
        return tables

    # single file
    _, ext = os.path.splitext(path.lower())
    if ext in (".csv",):
        df = pd.read_csv(path, dtype=str, low_memory=False)
        tables.append((os.path.basename(path), df))
    elif ext in (".xls", ".xlsx"):
        # read all sheets
        xls = pd.ExcelFile(path)
        for sheet in xls.sheet_names:
            df = pd.read_excel(path, sheet_name=sheet, dtype=str)
            tables.append((f"{os.path.basename(path)}::{sheet}", df))
    elif ext in (".json",):
        # try to load as records
        df = pd.read_json(path, dtype=False)
        if isinstance(df, dict):
            # maybe a mapping or nested; try to normalize
            try:
                df = pd.json_normalize(df)
            except Exception:
                df = pd.DataFrame([df])
        tables.append((os.path.basename(path), df.astype(str)))
    else:
        # not supported - return empty
        return []
    return tables

def infer_python_type(val):
    if val is None or (isinstance(val, float) and np.isnan(val)):
        return "null"
    if isinstance(val, str):
        v = val.strip()
        if v == "" or v.lower() in {"nan", "none", "null"}:
            return "null"
        if re.fullmatch(r"[+-]?\d+", v):
            return "int"
        if re.fullmatch(r"[+-]?\d+\.\d+", v):
            return "float"
        try:
            _ = pd.to_datetime(v, errors='coerce', utc=False)
            if not pd.isna(_):
                return "date"
        except Exception:
            pass
        return "str"
    if isinstance(val, (int, np.integer)):
        return "int"
    if isinstance(val, (float, np.floating)):
        if math.isnan(val):
            return "null"
        return "float"
    if isinstance(val, (pd.Timestamp,)):
        return "date"
    return "other"

def type_consistency(series: pd.Series) -> Tuple[float, Dict[str,int]]:
    types = Counter()
    for v in series:
        t = infer_python_type(v)
        types[t] += 1
    total = sum(types.values()) - types.get("null", 0)
    if total <= 0:
        return 0.0, dict(types)
    most_common = max(( (k, c) for k,c in types.items() if k!="null" ), key=lambda x:x[1], default=(None,0))
    ratio = most_common[1] / total if total>0 else 0.0
    return float(ratio), dict(types)

def length_stats(series: pd.Series) -> Dict[str, float]:
    lengths = [len(str(x)) for x in series.dropna().astype(str)]
    if len(lengths)==0:
        return {"mean":0.0,"std":0.0,"cv":1.0,"median":0.0,"iqr":0.0}
    arr = np.array(lengths, dtype=float)
    mean = float(arr.mean())
    std = float(arr.std(ddof=0))
    cv = float(std/mean) if mean>0 else 1.0
    q75, q25 = np.percentile(arr, [75,25])
    iqr = float(q75 - q25)
    return {"mean":mean, "std":std, "cv":cv, "median":float(np.median(arr)), "iqr":iqr}

def missing_rate(series: pd.Series) -> float:
    total = len(series)
    if total == 0:
        return 1.0
    missing = series.isnull() | series.astype(str).str.strip().isin(["", "nan", "none", "null"])
    return float(missing.sum() / total)

def unique_ratio(series: pd.Series) -> float:
    total = len(series)
    if total==0:
        return 0.0
    return float(series.dropna().astype(str).nunique() / total)

def shannon_entropy(series: pd.Series) -> float:
    vals = [str(x) for x in series.dropna().astype(str)]
    if len(vals)==0:
        return 0.0
    cnt = Counter(vals)
    total = sum(cnt.values())
    ent = 0.0
    for c in cnt.values():
        p = c/total
        ent -= p * math.log2(p)
    return float(ent)

def format_match_rate(series: pd.Series, patterns: Dict[str,str]) -> Tuple[float, Dict[str,float]]:
    rates = {}
    total = 0
    match_counts = Counter()
    for v in series.dropna().astype(str):
        total += 1
        s = v.strip()
        for name, patt in patterns.items():
            if re.fullmatch(patt, s):
                match_counts[name] += 1
                break
    if total == 0:
        return 0.0, {}
    for name in patterns.keys():
        rates[name] = match_counts[name] / total
    best = max(rates.values()) if rates else 0.0
    return float(best), {k: float(v) for k,v in rates.items()}

def normalize_value(s: str) -> str:
    if s is None:
        return ""
    t = str(s).strip().lower()
    t = re.sub(r"[\s\-\.\,/_]+", "", t)
    return t

def stable_after_normalization(series: pd.Series) -> float:
    total = len(series)
    if total == 0:
        return 0.0
    orig_unique = series.dropna().astype(str).nunique()
    norm = series.fillna("").astype(str).map(normalize_value)
    norm_unique = norm.nunique()
    if orig_unique == 0:
        return 0.0
    return float(norm_unique / orig_unique)

# --------------------------- Scoring per column ---------------------------
def score_column(series: pd.Series, config=DEFAULT_CONFIG) -> Tuple[float, Dict[str,Any]]:
    th = config["thresholds"]
    w = config["weights"]
    res = {}
    tc_ratio, tc_breakdown = type_consistency(series)
    ls = length_stats(series)
    miss = missing_rate(series)
    uniq = unique_ratio(series)
    ent = shannon_entropy(series)
    fmt_best, fmt_rates = format_match_rate(series, config["formats"])
    stable_norm = stable_after_normalization(series)

    score = 0.0
    details = {}
    details["type_consistency_ratio"] = tc_ratio
    score += w["type_consistency"] * min(1.0, tc_ratio / max(1e-9, th["type_consistency_ratio"]))
    details["format_best_rate"] = fmt_best
    score += w["format_match"] * min(1.0, fmt_best / max(1e-9, th["format_match_ratio"]))
    details["length_cv"] = ls["cv"]
    cvscore = max(0.0, 1.0 - min(ls["cv"], 2.0) / max(th["length_cv"], 0.0001))
    score += w["length_consistency"] * cvscore
    details["missing_rate"] = miss
    miss_score = max(0.0, 1.0 - miss / max(th["max_missing_rate"], 1e-9))
    score += w["missing_rate"] * miss_score
    details["unique_ratio"] = uniq
    uniq_score = min(1.0, uniq / max(1e-9, th["min_unique_ratio"]))
    score += w["unique_ratio"] * uniq_score
    details["entropy"] = ent
    ent_score = min(1.0, ent / max(1e-9, th["min_entropy"]))
    score += w["entropy"] * ent_score
    details["stable_norm_ratio"] = stable_norm
    stable_score = min(1.0, stable_norm / max(1e-9, th["stable_norm_ratio"]))
    score += w["stable_over_norm"] * stable_score

    max_possible = sum(w.values())
    total_pct = float(score / max_possible * 100.0) if max_possible>0 else 0.0

    details.update({
        "type_breakdown": tc_breakdown,
        "length_stats": ls,
        "format_rates": fmt_rates,
    })
    return total_pct, details

# --------------------------- Main analyzer ---------------------------
def analyze_column(args):
    col, series, config = args
    pct, det = score_column(series, config)
    return col, round(pct, 2), det

def analyze_table(df: pd.DataFrame, config=DEFAULT_CONFIG) -> Dict[str,Any]:
    out = {"nrows": len(df), "ncols": len(df.columns), "columns": {}}
    tasks = [(col, df[col], config) for col in df.columns]

    with ThreadPoolExecutor(max_workers=15) as executor:
        futures = [executor.submit(analyze_column, t) for t in tasks]
        for f in as_completed(futures):
            col, pct, det = f.result()
            out["columns"][col] = {"score": pct, "details": det}
        
    # for col in df.columns:
    #     series = df[col]
    #     pct, det = score_column(series, config)
    #     out["columns"][col] = {"score": round(pct,2), "details": det}
    candidates = [col for col,info in out["columns"].items() if info["score"] >= config["score_threshold"]]
    if not candidates:
        sorted_cols = sorted(out["columns"].items(), key=lambda kv: kv[1]["score"], reverse=True)
        top3 = [k for k,_ in sorted_cols[:3]]
        candidates = top3
    out["candidates"] = candidates
    return out

def analyze_path(path: str, config=DEFAULT_CONFIG) -> Dict[str,Any]:
    tables = safe_load_file(path)
    if not tables:
        return {"error": "no supported files/tables found at path", "path": path}
    result = {}
    for name, df in tables:
        result[name] = analyze_table(df, config)
    return result

# --------------------------- CLI ---------------------------
def cli_main(argv):
    if len(argv) < 2:
        print("Usage: python analyze_pseudokey.py /path/to/file_or_directory")
        return 1
    path = argv[1]
    res = analyze_path(path)
    print(json.dumps(res, ensure_ascii=False, indent=2))
    return 0

if __name__ == "__main__":
    sys.exit(cli_main(sys.argv))
