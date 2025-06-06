import requests

res = requests.get('http://localhost:1234/pii-detection/detect/data_20250606180453500585')
print(res.text)
