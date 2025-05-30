import time
from typing import Dict, List
import pandas as pd
import requests
from dataclasses import dataclass


@dataclass
class EventDTO:
    event: str
    data: Dict


@dataclass
class TopicDTO:
    topic: str
    url: str


publish_event_url = "http://localhost:1780/event/publish"
subscribe_topic_url = 'http://localhost:1780/topic/subscribe'


def publish(event: str, data: Dict) -> bool:
    dto = EventDTO(event=event, data=data)
    res = requests.post(url=publish_event_url, json=dto.__dict__)

    # TODO: API Gateway가 잘 받았는지 확인해야 하고, 그렇지 않을 경우 재요청 구현 예정
    
    return res.status_code == 200


def subscribe(topic: str, callback_url: str, count: int = 1, interval: int = 1):
    try:
        if not topic or topic.isspace() or not callback_url or callback_url.isspace():
            raise Exception('topic과 callback_url은 필수입니다.')
            
        # n번의 재시도 과정에서 interval이 0 이하인 경우 default 10초로 설정
        if (count > 1) and (interval <= 0):
            interval = 10

        topic = TopicDTO(topic=topic, url=callback_url)
        for i in range(count):
            res = requests.post(url=subscribe_topic_url, json=topic.__dict__)
            if res.status_code == 200:
                return
        
            print(f"[debug]: {topic} {i + 1}번 째 등록 실패")
            time.sleep(interval)

        raise Exception(f'{topic} 구독 실패: {res.text}')
        
    except Exception as e:
        raise Exception(f'subscribe() 실패 {e}') from e


def request_get_columns(dataset_info: str) -> List[str] | None:
    """ 
    API Gateway에 데이터셋의 컬럼 정보를 요청하는 함수.
        - dataset_info: 데이터셋의 정보(파일명 등)를 포함하는 문자열
        - return: 데이터셋의 컬럼 정보가 포함된 리스트 혹은 None
    """
    response = requests.get(url='http://localhost:1780/data/columns/' + dataset_info)
    if response.status_code != 200:
        return None
    
    return response.json()


def request_get_column_data(dataset_info: str, columns: List[str]) -> pd.DataFrame:
    response = requests.post(
        url='http://127.0.0.1:1780/get-column-data',
        json={
            "title": dataset_info,
            "columns": columns
        }
    )
    if response.status_code == 200:
        return pd.DataFrame(response.json())
    else:
        print(f"[debug]: get_column_data 실패: {response.text}")