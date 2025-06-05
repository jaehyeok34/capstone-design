import pandas as pd

pd.read_csv('data2.csv')\
    .sample(frac=1, random_state=42) \
    .reset_index(drop=True) \
    .to_csv('data3.csv', index=False)