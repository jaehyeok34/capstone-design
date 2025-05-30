import sys, os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from api_gateway_utils import publish

ok, text = publish(
    name='pii.detection.request',
    path_variable="data1_20250529213414351424.csv",
    jsonData=None
)

print(ok, text)