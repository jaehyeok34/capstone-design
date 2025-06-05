import requests

url = 'http://localhost:1780'

def csv_register():
    # /csv/register
    file = {'file': open('data3.csv', 'rb')}
    res = requests.post(url+'/csv/register', files=file)
    print(res.text)

csv_register()