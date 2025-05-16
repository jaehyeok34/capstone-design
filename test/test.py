import requests
import threading

def request():
    url = "http://localhost:1780/"

    response = requests.post(url+"register", json={'topic': 'test', 'url': 'http://127.0.0.1:1782/detect'})
    print(response.status_code)
    print(response.text)

    
# threading.Thread(target=request).start()
request()