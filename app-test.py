import requests

url = "http://127.0.0.1:5000/detect"
data = ["example1", "example2", "example3"]

response = requests.post(url, json=data)

print("Status Code:", response.status_code)
print("Response Body:", response.text)