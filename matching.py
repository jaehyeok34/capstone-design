import json
from api_gateway_utils import publish_event

publish_event(
    name='matching.match.request',
    path_variable='matching_result',
    json_data=json.dumps(['mk_data1_20250605075435758569_20250605075844024088', 'mk_data3_20250605083838189599_20250605083851269105'])
)