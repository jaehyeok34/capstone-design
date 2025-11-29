from py_client.agent import Agent


def main():
    with Agent.of(host="localhost", port=3401, client_id="export_test") as agent:
        topic_name = "convert_file"
        partition = 2

        custom_option = {
            "file.format": "csv",
            "file.name": "sample.md"
        }

        response = agent.producer.syncProduce(topic_name, str(partition), custom_option)
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