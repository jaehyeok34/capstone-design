import json
from api_gateway_utils import publish_event
import requests

url = 'http://localhost:1780'

def csv_register():
    # /csv/register
    info = []

    for f in ['data1.csv', 'data3.csv']:
        file = {'file': open(f, 'rb')}
        
        res = requests.post(url+'/csv/register', files=file)
        print(res.text)
        if res.status_code == 200:
            info.append(res.text)
            
    return info



publish_event(
    name='pii.detection.request',
    json_data=json.dumps(csv_register())
)