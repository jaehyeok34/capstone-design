import time
from typing import Dict, List, Any
import pandas as pd
import requests
from dataclasses import dataclass

@dataclass
class EventDTO:
    event: str
    data: Dict[str, List[str]]


topic_subscribe_url = 'http://localhost:1780/subscribe-topic'

def request_post(target_url: str, event: str, data: Dict[str, Any]) -> bool:
    dto = EventDTO(event=event, data=data)
    response = requests.post(url=target_url, json=dto.__dict__)

    # API Gateway가 잘 받았는지 확인해야 하고, 그렇지 않을 경우 재요청 해야하는데,
    # 시간이 없으니까 일단은 one line path만 구성
    
    return response.status_code == 200


def subscribe(topic: str, callback_url: str, end_point_url: str = topic_subscribe_url, count: int = 1, interval: int = 0) -> bool:
    default_interval = 10
    interval = max(0, interval)

    # 매개변수 체크
    if (not topic) or (not callback_url):
        raise ValueError('"topic"과 "callback_url"은 필수입니다.')
    
    # n번의 재시도 과정에서 interval이 0인 경우 default 10초로 설정
    if (count > 1) and (interval == 0):
        interval = default_interval

    for i in range(count):
        try:
            response = requests.post(url=end_point_url, json={'topic': topic, 'url': callback_url})
            if response.status_code == 200:
                print(f"[debug]: gateway에 {topic} 등록 성공")
                return True
            
        except Exception:
            pass

        print(f"[debug]: gateway에 {topic} {i}번 째 등록 실패")
        time.sleep(interval)
        
    return False


def request_get_columns(selectedRegisteredDataTitle: str) -> List[str]:
    response = requests.post(url='http://127.0.0.1:1780/get-columns', data=selectedRegisteredDataTitle)
    if response.status_code == 200:
        return response.json()
    else:
        print(f"[debug]: get_columns 실패: {response.text}")


def request_get_column_data(selectedRegisteredDataTitle: str, columns: List[str]) -> pd.DataFrame:
    response = requests.post(
        url='http://127.0.0.1:1780/get-column-data',
        json={
            "title": selectedRegisteredDataTitle,
            "columns": columns
        }
    )
    if response.status_code == 200:
        return pd.DataFrame(response.json())
    else:
        print(f"[debug]: get_column_data 실패: {response.text}")