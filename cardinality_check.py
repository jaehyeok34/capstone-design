import pandas as pd

def is_potential_identifier(series, min_ratio=0.95):
    """
    고유값 비율(min_ratio) 기준으로 식별정보 여부 판단
    :param series: pd.Series
    :param min_ratio: 고유값 비율 기준 (L / N)
    :return: bool
    """
    unique_count = series.nunique(dropna=True)
    total_count = series.shape[0]

    if total_count == 0:
        return False

    cardinality_ratio = unique_count / total_count

    return cardinality_ratio >= min_ratio

def adjust_threshold_by_join_rate(join_rate, base_threshold=0.95):
    """
    결합률에 따라 고유값 비율 기준을 동적으로 조절
    :param join_rate: float (0~1)
    :param base_threshold: 기준값
    :return: 조정된 기준값
    """
    if join_rate < 0.2:
        return base_threshold - 0.1
    elif join_rate < 0.5:
        return base_threshold - 0.05
    else:
        return base_threshold

def analyze_dataframe(df, min_ratio=0.95, join_rate=None):
    """
    데이터프레임 각 컬럼에 대해 식별정보 여부를 판단
    :param df: pd.DataFrame
    :param min_ratio: 고유값 비율 기준
    :param join_rate: 결합률 정보가 있을 경우 동적 기준 조절
    :return: 식별정보로 판단된 컬럼 목록
    """
    identifier_columns = []
    # print(f"min_ratio: {min_ratio}")

    # 결합률 기반으로 min_ratio 조정
    if join_rate is not None:
        min_ratio = adjust_threshold_by_join_rate(join_rate, min_ratio)

    for column in df.columns:
        series = df[column]
        if is_potential_identifier(series, min_ratio=min_ratio):
            identifier_columns.append(column)

    return identifier_columns

if __name__ == "__main__":
    df = pd.read_csv("data_files/datas.csv", encoding='utf-8')

    identifiers = analyze_dataframe(df, min_ratio=0.6, join_rate=0.3)
    
    print("식별정보로 판단된 컬럼:", identifiers)