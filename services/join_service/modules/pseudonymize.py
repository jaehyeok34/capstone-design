import os
import re
import math
from typing import Optional
import sys

import numpy as np
import pandas as pd


def infer_column_type(series: pd.Series) -> str:
    s = series.dropna().astype(str)
    if s.empty:
        return 'unknown'
    # numeric-like
    try:
        _ = pd.to_numeric(s.astype(str).str.replace(',',''))
        return 'numeric'
    except Exception:
        pass

    sample = s.iloc[0]
    # phone number heuristics
    if re.match(r'^[0-9*\-]{9,13}$', sample) and re.search(r'0\d{1,2}', sample):
        # likely phone (010-1234-5678, masked variants allowed)
        return 'phone'

    # 차량번호 heuristics: digits + hangul + digits (masked variants allowed)
    if re.search(r'\d{1,4}[가-힣*]+\d{1,4}', sample):
        return 'car_number'

    # 주민등록번호 like: 6digits-7digits or masked variants
    if re.match(r'^[0-9*]{6}-[0-9*]{7}$', sample):
        return 'rrn'
    # also accept pure 13-digit RRN without hyphen (or masked variants)
    if re.match(r'^[0-9*]{13}$', sample):
        return 'rrn'

    # 주소 heuristics: contains 시/도/구/군 or road keywords
    if any(k in sample for k in ['시', '도', '군', '구', '로', '길', '읍', '면']):
        return 'address'

    # name heuristic: mostly hangul and short
    hangul_chars = sum(1 for ch in sample if '\uac00' <= ch <= '\ud7a3')
    if hangul_chars >= 1 and len(sample) <= 5:
        return 'name'

    return 'string'


def human_readable_range(lower: float, upper: float) -> str:
    # convert to human friendly using '원','만원','억' when appropriate
    def fmt(x):
        if x >= 10000:
            # show in 억 or 만원
            if x >= 10000 * 10000:
                return f"{x/100000000:.0f}억"
            return f"{int(x):,}원"
        return f"{int(x):,}원"

    return f"{int(lower):,}~{int(upper):,}"


def categorize_numeric(series: pd.Series, base_unit: int = 1000) -> pd.Series:
    """
    Numeric categorization producing ranges aligned to `base_unit` multiples.

    Example: base_unit=1000 -> bins like 0~1000, 1000~2000, ...

    If the naive number of bins would be very large, the function increases the
    bin width by powers of 10 until the number of bins is <= 50 to avoid huge
    label cardinality.
    """
    s = series.astype(str).str.replace(',','')
    numeric = pd.to_numeric(s, errors='coerce')
    if numeric.dropna().empty:
        return series

    # If series looks already categorized (contains '대' or '~'), coarsen by
    # extracting the numeric seed and widening the range by one base_unit multiple.
    contains_cat = series.dropna().astype(str).str.contains(r'[대~]|~', regex=True).any()
    if contains_cat:
        parsed = []
        for val in series.astype(str):
            m = re.search(r'(\d{1,12})', val)
            if m:
                parsed.append(float(m.group(1)))
            else:
                parsed.append(np.nan)
        pnum = pd.Series(parsed, index=series.index)
        labels = []
        for x in pnum:
            if pd.isna(x):
                labels.append('')
            else:
                low = int((x // base_unit) * base_unit)
                high = low + base_unit
                labels.append(f"{low}~{high}")
        return pd.Series(labels, index=series.index)

    # Determine floor/ceil using base_unit
    mn = int(math.floor(numeric.dropna().min() / base_unit) * base_unit)
    mx = int(math.ceil(numeric.dropna().max() / base_unit) * base_unit)
    if mn == mx:
        # single value -> return a single range
        return pd.Series([f"{mn}~{mx}"] * len(series), index=series.index)

    # compute initial number of bins
    n_bins = max(1, (mx - mn) // base_unit)

    # avoid excessive bins: increase width by powers of 10 until bins <= 50
    width = base_unit
    while n_bins > 50:
        width *= 10
        n_bins = max(1, (mx - mn) // width)

    # build bins
    bins = list(range(mn, mx + width, width))
    if len(bins) <= 1:
        # fallback to two bins
        bins = [mn, mx]

    labels = [f"{bins[i]}~{bins[i+1]}" for i in range(len(bins)-1)]
    out = pd.cut(numeric, bins=bins, labels=labels, include_lowest=True)
    return out.astype(str).fillna('')


def categorize_age(series: pd.Series) -> pd.Series:
    """Bucket ages into decade ranges like '0~9','10~19','20~29', etc.

    Behavior:
    - If a value already matches the decade range pattern (e.g. '10~19'), keep it as-is.
    - If the value is numeric, bucket into decades.
    - If the value cannot be parsed, keep the original string (do not erase).
    - Values outside 0-120 are labeled 'unknown'.
    """
    labels = []
    for val in series.astype(str):
        if val is None:
            labels.append('')
            continue
        s = val.strip()
        if s == '':
            labels.append('')
            continue
        # if already a decade range like '10~19', keep as-is
        if re.match(r'^\d{1,3}~\d{1,3}$', s):
            labels.append(s)
            continue
        if s.lower() == 'unknown':
            labels.append('unknown')
            continue
        # try to extract a numeric token
        m = re.search(r"(\d{1,3})", s)
        if not m:
            # cannot parse -> preserve original value rather than blanking
            labels.append(s)
            continue
        try:
            iv = int(m.group(1))
        except Exception:
            labels.append(s)
            continue
        if iv < 0 or iv > 120:
            labels.append('unknown')
            continue
        lower = (iv // 10) * 10
        upper = lower + 9
        labels.append(f"{lower}~{upper}")
    return pd.Series(labels, index=series.index)


def categorize_money(series: pd.Series) -> pd.Series:
    """Convert monetary values to left-digit bucket with trailing zeros and append '대'.

    Examples:
    - 42478000 -> 40000000대
    - '42478000~52478000' -> 40000000대 (uses the lower bound)
    - non-numeric values -> empty string
    """
    out = []
    for val in series.astype(str):
        if val is None:
            out.append('')
            continue
        s = val.strip()
        if s == '':
            out.append('')
            continue
        # extract first numeric token
        m = re.search(r"(\d{1,15})", s)
        if not m:
            out.append('')
            continue
        try:
            n = int(m.group(1))
        except Exception:
            out.append('')
            continue
        if n == 0:
            out.append('0대')
            continue
        digits = len(str(n))
        power = 10 ** (digits - 1)
        bucket = (n // power) * power
        out.append(f"{bucket}대")
    return pd.Series(out, index=series.index)


def mask_rrn_progressive(val: str) -> str:
    if not isinstance(val, str):
        val = str(val)
    s = val.strip()
    # normalize spacing
    s = s.replace(' ', '')
    # match pattern
    if not re.match(r'^[0-9*]{6}-[0-9*]{7}$', s):
        # if purely digits and length 13, insert hyphen
        if re.match(r'^[0-9]{13}$', s):
            s = s[:6] + '-' + s[6:]
        else:
            # fallback: mask middle portion progressively
            if '*' not in s:
                n = len(s)
                keep = max(1, n//3)
                return s[:keep] + '*'*(n-keep)
            else:
                # extend masking by hiding one more visible char from right
                pos = s.rfind('*')
                # replace previous visible char if exists
                for i in range(len(s)-1, -1, -1):
                    if s[i] != '*':
                        s = s[:i] + '*' + s[i+1:]
                        return s
                return s

    left, right = s.split('-')
    # If no * present at all -> first mask post-hyphen leave first char
    if '*' not in s:
        if len(right) >= 1:
            return f"{left}-{right[0]}{'*'*(len(right)-1)}"

    # If post-hyphen has some visible digits, mask them all
    if re.search(r'[0-9]', right):
        new_right = re.sub(r'[0-9]', '*', right)
        return f"{left}-{new_right}"

    # right is fully masked -> start masking left from the right in groups of 2
    left_list = list(left)
    # find rightmost unmasked (digit)
    idxs = [i for i, ch in enumerate(left_list) if ch != '*']
    if not idxs:
        return s
    # mask up to 2 rightmost unmasked
    mask_count = min(2, len(idxs))
    for i in range(mask_count):
        pos = idxs[-1 - i]
        left_list[pos] = '*'
    return f"{''.join(left_list)}-{'*'*len(right)}"


def mask_phone_progressive(val: str) -> str:
    # Robust phone masking with explicit grouping to avoid over-masking.
    if not isinstance(val, str):
        val = str(val)
    s = val.strip()
    if s == '':
        return s

    # normalize separators to hyphen
    # keep '*' as part of groups so masked groups are preserved
    sep_groups = re.split(r'[^0-9*]+', s)
    seps = re.findall(r'[^0-9*]+', s)

    # collapse empty tokens
    groups = [g for g in sep_groups if g != '']

    # helper to format groups with hyphens
    def fmt(g0, g1, g2):
        return f"{g0}-{g1}-{g2}"

    # operate on pure digit groups for deterministic behavior
    if len(groups) == 3 and all(re.fullmatch(r'[0-9*]+', g) for g in groups):
        g0, g1, g2 = groups
        # step1: if no masking at all, mask middle 4 digits (preserve first and last)
        if '*' not in g0 and '*' not in g1 and '*' not in g2:
            # ensure g1 masked length is 4 if possible, otherwise mask entire g1
            mask_mid = '*' * len(g1)
            return fmt(g0, mask_mid, g2)
        # step2: if middle masked but last not fully masked digits -> mask last group fully
        if all(ch == '*' for ch in g1) and not all(ch == '*' for ch in g2):
            return fmt(g0, g1, '*' * len(g2))
        # step3: if middle and last are masked, mask first group as well
        if all(ch == '*' for ch in g1) and all(ch == '*' for ch in g2):
            return fmt('*' * len(g0), g1, g2)

    # If no clear grouping, fallback to operate on digit string of length 10/11
    digits = re.sub(r'\D', '', s)
    if digits == '':
        return s
    if '*' not in s:
        if len(digits) == 11:
            a, b, c = digits[:3], digits[3:7], digits[7:]
            return fmt(a, '*' * len(b), c)
        if len(digits) == 10:
            a, b, c = digits[:3], digits[3:6], digits[6:]
            return fmt(a, '*' * len(b), c)
    # if already partially masked but no hyphens, try to detect masked groups
    # we look for pattern like 010****7180 or 010**** **** etc.
    # simple progressive fallback: mask rightmost unmasked group by group
    # break digits into 3 parts based on length
    if len(digits) in (10, 11):
        if len(digits) == 11:
            a, b, c = digits[:3], digits[3:7], digits[7:]
        else:
            a, b, c = digits[:3], digits[3:6], digits[6:]
        # if middle contains '*', then mask c if not masked, else mask a
        if '*' in s:
            if not all(ch == '*' for ch in c):
                return fmt(a, '*' * len(b), '*' * len(c))
            return fmt('*' * len(a), '*' * len(b), '*' * len(c))
    # final fallback: return original trimmed
    return s


def mask_car_progressive(val: str) -> str:
    if not isinstance(val, str):
        val = str(val)
    s = val.strip()
    if s == '':
        return s
    raw = re.sub(r'[^0-9가-힣*]', '', s)
    # if contains hangul (unmasked), replace first hangul with '*'
    m = re.search(r'[가-힣]', raw)
    if m:
        i = m.start()
        raw = raw[:i] + '*' + raw[i+1:]
        return raw

    # otherwise progressively mask rightmost visible digit
    for i in range(len(raw)-1, -1, -1):
        if raw[i] != '*':
            raw = raw[:i] + '*' + raw[i+1:]
            break
    return raw


def mask_address_progressive(val: str) -> str:
    if not isinstance(val, str):
        return val
    s = val.strip()
    if s == '':
        return s
    parts = s.split()
    if len(parts) <= 1:
        return s

    # Remove trailing numeric tokens (번지, 호 등). If there are multiple
    # trailing tokens that look like numbers (e.g. '17-16' and '503호'),
    # remove them all in the first step so that '... 복용북로 17-16 503호'
    # becomes '... 복용북로'.
    def is_address_number_token(tok: str) -> bool:
        t = tok.rstrip(',.')
        if re.match(r'^\d+(?:-\d+)*$', t):
            return True
        if re.match(r'^\d+호$', t):
            return True
        if re.match(r'^\d+번지$', t):
            return True
        # tokens containing digits and hyphen or ending with '호'
        if re.search(r'\d', t) and ('-' in t or t.endswith('호')):
            return True
        return False

    # remove as many trailing numeric-like tokens as present (at least one)
    new_parts = parts.copy()
    removed = 0
    while new_parts and is_address_number_token(new_parts[-1]):
        new_parts.pop()
        removed += 1

    if removed > 0:
        # if we removed tokens, return the shortened address
        return ' '.join(new_parts)

    # otherwise: remove last token (one level up)
    return ' '.join(parts[:-1])


def mask_name_progressive(val: str) -> str:
    if not isinstance(val, str):
        val = str(val)
    s = val.strip()
    if s == '':
        return s

    s_list = list(s)
    n = len(s_list)

    # 1) 초기 상태:
    # - 이름 길이 >= 4인 경우: 양끝 한 글자 제외한 나머지 전부 마스킹
    # - 그 외(2~3글자 등): 기존 동작(중간 글자 하나 마스킹)
    if '*' not in s:
        if n >= 4:
            for i in range(1, n-1):
                s_list[i] = '*'
            return ''.join(s_list)
        else:
            mid = n // 2
            s_list[mid] = '*'
            return ''.join(s_list)

    # 2) 이미 일부 마스킹된 상태: 오른쪽(끝)부터 다음 보이는 글자 하나를 마스킹
    visible_idxs = [i for i, ch in enumerate(s_list) if ch != '*']
    if not visible_idxs:
        return s
    # mask the rightmost visible character
    pos = visible_idxs[-1]
    s_list[pos] = '*'
    return ''.join(s_list)


def mask_string_general(val: str) -> str:
    # fallback masking: progressively increase '*' count from right
    if not isinstance(val, str):
        return val
    s = val
    if '*' not in s:
        n = len(s)
        keep = max(1, n//3)
        return s[:keep] + '*'*(n-keep)
    else:
        # mask one more visible char from right
        s_list = list(s)
        for i in range(len(s_list)-1, -1, -1):
            if s_list[i] != '*':
                s_list[i] = '*'
                break
        return ''.join(s_list)


def pseudonymize_dataframe(df: pd.DataFrame, columns: Optional[list] = None) -> pd.DataFrame:
    """Pseudonymize dataframe.

    If `columns` is provided (list of column names), only those columns are
    pseudonymized; other columns are left unchanged.
    """
    out = df.copy()

    # Normalize requested columns (strip spaces, lowercase) for tolerant matching
    norm_requested = None
    if columns is not None:
        norm_requested = set([str(c).strip().lower() for c in columns])
        # build mapping of normalized actual column names
        norm_actual = {str(c).strip().lower(): c for c in out.columns}
        missing = sorted(list(norm_requested - set(norm_actual.keys())))
        if missing:
            sys.stderr.write(f"Warning: 지정한 칼럼이 존재하지 않습니다: {', '.join(missing)}\n")

    for col in out.columns:
        series = out[col]
        # If columns filter provided, only process those columns (tolerant matching)
        if norm_requested is not None and str(col).strip().lower() not in norm_requested:
            continue

        ctype = infer_column_type(series)
        # special-case: column name indicating age -> decade buckets
        col_name = str(col)
        if col_name.strip() in ['나이', 'age', 'Age'] or '나이' in col_name:
            out[col] = categorize_age(series)
        # special-case: monetary columns
        elif col_name.strip() in ['소득','수입', '지출', 'income', 'expense']:
            out[col] = categorize_money(series)
        elif ctype == 'numeric':
            out[col] = categorize_numeric(series)
        elif ctype == 'rrn':
            out[col] = series.astype(str).apply(mask_rrn_progressive)
        elif ctype == 'address':
            out[col] = series.astype(str).apply(mask_address_progressive)
        elif ctype == 'phone':
            out[col] = series.astype(str).apply(mask_phone_progressive)
        elif ctype == 'car_number':
            out[col] = series.astype(str).apply(mask_car_progressive)
        elif ctype == 'name':
            out[col] = series.astype(str).apply(mask_name_progressive)
        else:
            out[col] = series.astype(str).apply(mask_string_general)
    return out


def load_table(path: str) -> pd.DataFrame:
    ext = os.path.splitext(path)[1].lower()
    if ext in ['.csv']:
        return pd.read_csv(path)
    if ext in ['.json']:
        return pd.read_json(path)
    if ext in ['.xls', '.xlsx']:
        return pd.read_excel(path)
    # fallback try csv
    return pd.read_csv(path)


def save_table(df: pd.DataFrame, path: str):
    ext = os.path.splitext(path)[1].lower()
    if ext in ['.csv']:
        df.to_csv(path, index=False)
    elif ext in ['.json']:
        df.to_json(path, orient='records', force_ascii=False)
    elif ext in ['.xls', '.xlsx']:
        df.to_excel(path, index=False)
    else:
        df.to_csv(path, index=False)


def pseudonymize_file(input_path: str, output_path: Optional[str] = None, columns: Optional[list] = None) -> str:
    """Pseudonymize a file by path and write output.

    Parameters:
    - input_path: path to input file (csv/json/xlsx)
    - output_path: optional path for output file. If not provided, will append
      `_pseudonymized` before extension.
    - columns: optional list of column names to pseudonymize (None -> all columns).

    Returns the output path.
    """
    df = load_table(input_path)
    out = pseudonymize_dataframe(df, columns=columns)
    if not output_path:
        base, ext = os.path.splitext(input_path)
        output_path = f"{base}_pseudonymized{ext or '.csv'}"
    save_table(out, output_path)
    return output_path


if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser(description='Pseudonymize a table (csv/json/excel)')
    parser.add_argument('input', help='Input file path')
    parser.add_argument('--output', '-o', help='Output file path (optional)')
    parser.add_argument('--columns', '-c', help='Comma-separated list of column names to pseudonymize (default: all columns)')
    args = parser.parse_args()
    cols = None
    if args.columns:
        # split by comma and strip whitespace
        cols = [c.strip() for c in args.columns.split(',') if c.strip()]
    # load table and apply pseudonymization with optional column filter
    outp = pseudonymize_file(args.input, output_path=args.output, columns=cols)
    print(f"Saved pseudonymized file to: {outp}")
