
import random
from py_client.agent import Agent
from services.get_projects_service import GET_PROJECTS

def main():
    with Agent.of(host='localhost', port=3401, client_id='get_tester') as agent:
        topic_name = "join_key"
        partition = GET_PROJECTS

        api_id = str(random.randint(1000, 9999))
        agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header={"api.id": api_id})

        if not (ok := agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition={"api.id": api_id})):
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