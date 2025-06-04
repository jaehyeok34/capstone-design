from flask import json
from api_gateway_utils import publish_event


publish_event(
    name='matching.match.request',
    path_variable='result',
    json_data=json.dumps(['mk_data2_20250602185128961493_20250602185543670365', 'mk_data1_20250602185117802834_20250602185543306168'])
)