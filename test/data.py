import json
import pandas as pd
import requests
from api_gateway_utils import publish_event

url = 'http://localhost:1780'

def csv_column_values(dataset_info):
    # /csv/column-values
    res = requests.post(url+'/csv/column-values/'+dataset_info, json=['name', 'ssn'])
    if res.status_code == 200:
        return res.json()
    
def csv_all_values(dataset_info):
    # /csv/all-values
    res = requests.get(url+'/csv/all-values/'+dataset_info)

    if res.status_code == 200:
        print(pd.DataFrame(res.json()).head(1))

def data_datasets():
    # /data/datasets
    res = requests.get(url+'/data/datasets')
    if res.status_code == 200:
        print(res.text)
        return json.loads(res.text)[0].get('datasetInfo')
    
    print(f"실패: {res.text}")
    return ""

def data_columns(dataset_info):
    # /data/columns
    res = requests.get(url+'/data/columns/'+dataset_info)
    if res.status_code == 200:
        return res.json()
    


dataset_info = data_datasets()

column_values = csv_column_values(dataset_info)
df1 = pd.DataFrame(column_values)
print(df1.head(1))

csv_all_values(dataset_info)

columns = data_columns(dataset_info)
print(columns)

