from api_gateway_utils import publish_event
dataset_info = 'data3_20250605083838189599'

publish_event(
    name='pii.detection.request',
    path_variable=dataset_info
)