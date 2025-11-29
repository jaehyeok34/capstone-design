import random
from py_client.agent import Agent
from services.create_ci_service import CREATE_CI


def main():
    with Agent.of(host="localhost", port=3401, client_id="create_ci_tester") as agent:
        topic_name = "join_key"
        partition = CREATE_CI

        api_id = str(random.randint(0, 9999))
        header = {
            "api.id": api_id,
            "project.id": "f1ab2b11-0b63-4dec-b022-c81904ab23bb"
        }
        agent.producer.asyncProduce(topic_name=topic_name, partition=str(partition), header=header)    
        if not agent.find_and_seek(topic_name=topic_name, partition=str(-CREATE_CI), condition=header):
            print("find and seek 실패")
            return
        
        consumed = agent.consumer.consume(topic_name=topic_name, partition=str(-CREATE_CI))[0]
        if error := consumed.get_header("error"):
            print(f"Error from create_ci_service: {error}")
            return
        
        print(f"응답 헤더: {consumed.header}")
        print(f"응답 페이로드: {consumed.payload}")

if __name__ == "__main__":
    main()