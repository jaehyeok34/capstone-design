import requests

def request():
    url = "http://localhost:1780/event"
    response = requests.post(url, json={'event': 'pii-detection.request', 'data': {'columns': ['name', 'email', 'phone']}})
    print(response.status_code)

request()