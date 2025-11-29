
import json
from project_manager import PREVIEW
from py_client.agent import new_agent
from py_client.message.message import Message


host, port = 'localhost', 3401
client_id = 'tester'
topic_name = 'join_key'
with new_agent(host, port, client_id) as agent:
    message = Message().add_options({
        'topic_name': topic_name,
        'partition': -PREVIEW
    })

    q, e, t = agent.notify(message)
    if t is None:
        raise RuntimeError()
    
    message.add_options({
        'partition': PREVIEW,
        'payload': json.dumps({
            'projectId': '1a95bdbb-9fb4-45b4-a508-9f9278594e9b',
            'fileName': 'dataA.csv'
        }).encode('utf-8')
    })

    message = agent.producer.syncProduce(message)

    assert q is not None
    message = agent.consumer.consume(q.get())

    print(message.option_as_str('payload'))
    


