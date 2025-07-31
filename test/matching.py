import json
from api_gateway_utils import publish_event

publish_event(
    name='matching.match.request',
    path_variable='matching_result',
    json_data=json.dumps(['mk_data1_20250606143652778103_20250606143654201587', 'mk_data3_20250606143605551165_20250606143607741380'])
)