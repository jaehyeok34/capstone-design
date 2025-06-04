import json
from api_gateway_utils import publish_event

for path_variable in ['data1_20250602185117802834', 'data2_20250602185128961493']:
    publish_event('pii.detection.request', path_variable, json.dumps(['name', 'ssn']))

print('event published')