from api_gateway_utils import publish_event
import requests

url = 'http://localhost:1780'

def csv_register():
    # /csv/register
    file = {'file': open('data3.csv', 'rb')}
    res = requests.post(url+'/csv/register', files=file)
    print(res.text)
    return res.text



publish_event(
    name='pii.detection.request',
    path_variable=csv_register()
)