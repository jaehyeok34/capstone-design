import json
from api_gateway_utils import publish_event

publish_event('test_event', 'data1_20250529213437538264.csv', json.dumps("helo"))

print('event published')