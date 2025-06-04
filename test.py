import pandas as pd


df = pd.read_csv('test.csv')
df.to_csv('test2.csv', index=False, encoding='utf-8-sig')