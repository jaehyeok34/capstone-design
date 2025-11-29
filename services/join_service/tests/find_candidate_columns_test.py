import base64
import json
from pathlib import Path
import random
from py_client.agent import Agent

def main():
    with Agent.of(host='localhost', port=3401, client_id='find_test') as agent:
        topic_name = "join_key"
        partition = 1

        with open("resources/data1.csv", "rb") as f1, open("resources/data4.csv", "rb") as f2:
            files = {
                Path(f1.name).name: base64.b64encode(f1.read()).decode("utf-8"),
                Path(f2.name).name: base64.b64encode(f2.read()).decode("utf-8"),
            }

        api_id = random.randint(1000, 9999)
        payload = json.dumps(files, ensure_ascii=False)
        response = agent.producer.syncProduce(
            topic_name=topic_name, partition=str(partition),
            header={"api.id": str(api_id)}, payload=payload
        )
        
        if not (ok := agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition={"api.id": str(api_id)})):
            print("find_and_seek() 실패")
            return

        response = agent.consumer.consume(topic_name, str(-partition))[0]
        if error := response.get_header("error"):
            print(f"오류: {error}")
            return

        print("응답 헤더:", response.header)
        print("응답 페이로드:", response.payload.decode('utf-8'))

if __name__ == '__main__':
    main()