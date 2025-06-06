import requests

res = requests.get('http://localhost:1234/pii-detection/detect/data3_20250605083838189599')
print(res.text)