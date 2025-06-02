from flask import json
import requests

data = ['name', 'ssn']

res = requests.post(
    'http://localhost:1783/matching-key/generate/data1_20250529213309454778.csv',
    json=data
)

if res.status_code != 200:
    print(res.text)

print(res.text)