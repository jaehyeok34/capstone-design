import math
import pandas as pd

def pseudonymize_object(df: pd.DataFrame, column: str, rate: int = 0.7):
    def partial_masking(x):
            if isinstance(x, str):
                mask_len = math.ceil(int(len(x) * rate))
                return x[:len(x)-mask_len] + '*' * mask_len
            
            return x
    df[column] = df[column].apply(partial_masking)


def pseudonymize_numeric(df: pd.DataFrame, column: str):
    n = len(df[column])
    q = max(1, n // 2)
    bins = pd.qcut(df[column], q=q, duplicates='drop')
    df[column] = bins.apply(lambda x: f'{x.left}~{x.right}')


df = pd.read_csv('datas.csv')

# "컬럼명": "타입"의 딕셔너리 구성
for column, dtype in df.dtypes.items():
    if dtype == 'object':
        pseudonymize_object(df, column)
    elif dtype == 'int64' or dtype == 'float64':
        pseudonymize_numeric(df, column)

df.to_csv('masked_datas.csv', index=False)