import json
from api_gateway_utils import publish_event

publish_event('pii.detection.request', 'data_20250602165256241574', json.dumps(['name', 'ssn']))

print('event published')