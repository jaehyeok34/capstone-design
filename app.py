import math
import pandas as pd

df = pd.read_csv('datas.csv')

print(df.dtypes) 

# "컬럼명": "타입"의 딕셔너리 구성
for column, dtype in df.dtypes.items():
    # object 타입은 마스킹 적용
    if dtype == 'object':
        def partial_masking(x):
            if isinstance(x, str):
                mask_len = math.ceil(int(len(x) * 0.7))
                return x[:len(x)-mask_len] + '*' * mask_len
            
            return x
        df[column] = df[column].apply(partial_masking)

df.to_csv('masked_datas.csv', index=False)