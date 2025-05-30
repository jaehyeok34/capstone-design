import requests

res = requests.get('http://localhost:1782/pii-detection/data1_20250529213414351424.csv')
print(res.text)