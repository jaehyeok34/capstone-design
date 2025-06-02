import requests
import pandas as pd
import json

res = requests.get('http://localhost:1780/csv/all-values/data1_20250529213309454778.csv')
if res.status_code != 200:
    print(res.text)
