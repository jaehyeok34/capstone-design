import requests

url = "http://localhost:1780/csv/register"
files = [
    ('file', open('data2.csv', 'rb')),
]
res = requests.post(url, files=files)

print(res.status_code, res.text)