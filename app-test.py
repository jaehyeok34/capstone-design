import pandas as pd
import requests

pii_data = {
    "name": ["John Doe", "Jane Smith"],
    "email": ["myemail@email.com", "mail@email.com"],
    "phone": ["123-456-7890", "098-765-4321"],
    "age": [30, 25],
    "address": ["123 Main St", "456 Elm St"],
    "city": ["New York", "Los Angeles"],
    "state": ["NY", "CA"],
    "zip": ["10001", "90001"]
}

url = "http://localhost:1783/gen-matching-key"
response = requests.post(url, json={"pii-data": pii_data})
print(response.status_code)
print(pd.DataFrame(response.json()))