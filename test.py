
import requests


res = requests.post(
    url="http://127.0.0.1:1780/event", 
    json={
        "event": "matching-key.generate.request",
        "data": {
            "piiColumns": {
                "datas.csv": ['email', 'name', 'ssn']
            }
         }
    }
)

print(res.status_code)
print(res.text)