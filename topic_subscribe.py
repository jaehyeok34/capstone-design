from api_gateway_utils import subscribe_topic

subscribe_topic(
    topic_name='test_event',
    callback_url='http://localhost:2121/test',
    method='POST',
    use_path_variable=True,

    count=3,
    interval=10
)