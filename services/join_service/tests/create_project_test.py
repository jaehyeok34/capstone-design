
import base64
import json
import random
from services.create_project_service import CREATE_PROJECT
from py_client.agent import Agent

def main():
    with Agent.of(host='localhost', port=3401, client_id='project_test') as agent:
        topic_name = "join_key"
        partition = CREATE_PROJECT

        with open("resources/data1.csv", "rb") as f1, open("resources/data2.csv", "rb") as f2, open("resources/data4.csv", "rb") as f4:
            file1 = f1.read()
            file2 = f2.read()
            file4 = f4.read()

        header = {"api.id": str(random.randint(1000, 9999))}
        payload = json.dumps({
            "projectName": f"my_project_{random.randint(1000,9999)}",
            "candidateColumns": {},
            "files": {
                "file1.csv": base64.b64encode(file1).decode("utf-8"),
                "file2.csv": base64.b64encode(file2).decode("utf-8"),
                "file4.csv": base64.b64encode(file4).decode("utf-8")
            }
        })

        response = agent.producer.syncProduce(
            topic_name=topic_name, partition=str(partition),
            header=header, payload=payload
        )
        
        if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header): 
            print("find_and_seek 실패")
            return
        
        response = agent.consumer.consume(topic_name, str(-partition))[0]
        if error := response.get_header("error"):
            print(f"오류 발생: {error}")

        print("응답 헤더:", response.header)
        print("응답 페이로드", response.payload.decode("utf-8"))

if __name__ == '__main__':
    main()