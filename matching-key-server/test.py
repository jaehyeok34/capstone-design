from flask import json
import requests

data = ['name', 'ssn']

res = requests.post(
    'http://localhost:1783/matching-key/generate/data_20250602165256241574',
    json=data
)

if res.status_code != 200:
    print(res.text)

print(res.text)