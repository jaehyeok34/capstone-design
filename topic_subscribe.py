import requests

topic = {
    'topic': 'test_topics',
    'url': 'http://example.exas'
}

res = requests.post('http://localhost:1780/topic/subscribe', json=topic)
if res.status_code != 200:
    print(res.text)
    exit(1)

print(res.status_code, res.text)