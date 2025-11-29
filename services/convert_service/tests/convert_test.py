from py_client.agent import Agent

def main():
    with Agent.of(host="localhost", port=3401, client_id="test_client") as agent:
        topic_name = "convert_file"
        partition = 1

        custom_option = {
            "file.name": "sample.csv",
            "api.id": "test_api"
        }
        
        with open("data.csv", "rb") as f:
            payload = f.read()

        response = agent.producer.syncProduce(topic_name, str(partition), custom_option, payload)
        if response.get_header("ok", "false").lower() != "true":
            print("produce 실패")
            return
        
        ok = agent.find_and_seek(topic_name, str(-partition), custom_option)
        if not ok:
            print("seek 실패")
            return
        
        response = agent.consumer.consume(topic_name, str(-partition))[0]
        if response.get_header("error"):
            print("consume 실패: " + response.get_header("error"))
            return
        
        print("응답 헤더:", response.header)
        print("응답 페이로드:", len(response.payload.decode("utf-8")))


if __name__ == "__main__":
    main()