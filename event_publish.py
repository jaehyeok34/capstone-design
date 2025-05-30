import requests

event = {
    "event": "test_topics",
    "data": None
}

res = requests.post('http://localhost:1780/event/publish', json=event)
if res.status_code != 200:
    print(res.text)
    exit(1)

print(res.status_code, res.text)