import requests

url = "http://127.0.0.1:1782/detect"
data = ["example1", "example2", "example3"]

response = requests.post(url, json=data)

print("Status Code:", response.status_code)
print("Response Body:", response.json())

print("재요청")
response = requests.post(url, json={'columns': data})
print("Status Code:", response.status_code)
print("Response Body:", response.json())