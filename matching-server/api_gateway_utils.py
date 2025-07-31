# latest: 06.07.4

import time
from typing import List, Literal, Optional
import pandas as pd
import requests
from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase
import os


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class EventDTO:
    name: str
    path_variable: str
    json_data: str


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class TopicDTO:
    name: str
    url: str
    method: Literal['GET', 'POST']
    use_path_variable: bool


host = os.getenv('API_GATEWAY_HOST', 'localhost')
port = os.getenv('API_GATEWAY_PORT', '1780')
event_uri = os.getenv('API_GATEWAY_EVENT_URI', '/event/publish')
topic_uri = os.getenv('API_GATEWAY_TOPIC_URI', '/topic/subscribe')

api_gateway_url = f'http://{host}:{port}'
publish_event_url = f'http://{host}:{port}{event_uri}'
subscribe_topic_url = f'http://{host}:{port}{topic_uri}'


def publish_event(name: str, path_variable: str = None, json_data: str = None) -> bool:
    event = EventDTO(name=name, path_variable=path_variable, json_data=json_data)
    res = requests.post(url=publish_event_url, json=event.to_dict())

    # TODO: API Gateway가 잘 받았는지 확인해야 하고, 그렇지 않을 경우 재요청 구현 예정
    return res.status_code == 200, res.text


def subscribe_topic(
    name: str, 
    callback_url: str, 
    method: Literal['GET', 'POST'],
    use_path_variable: bool = False,

    count: int = 1, 
    interval: int = 1
):
    # 매개변수 검증
    if (
        (not name or name.isspace()) or 
        (not callback_url or callback_url.isspace()) or
        (not method or method.upper() not in ['GET', 'POST'])
    ):
        raise Exception('subscribe_topic() topic, callback_url, method는 필수입니다.(method는 GET 또는 POST)')
        
    # n번의 재시도 과정에서 interval이 0 이하인 경우 default 10초로 설정
    if (count > 1) and (interval <= 0):
        interval = 10

    # topic 생성
    topic = TopicDTO(
        name=name, 
        url=callback_url, 
        method=method.upper(), 
        use_path_variable=use_path_variable
    )

    # n번 재시도
    for i in range(count):
        # to_json()은 dataclass_json 라이브러리에서 제공하는 메서드(자동으로 camel case로 변환)
        try:
            res = requests.post(url=subscribe_topic_url, json=topic.to_dict())
            if res.status_code != 200:
                # message = json.loads(res.text)
                raise Exception(f"[debug] {i + 1}번 째 구독 실패: {res.text}")
            
            return 
        except Exception as e:
            print(f"[debug] {e}")
            time.sleep(interval)
    
    # n번의 재시도에도 실패한 경우 예외 발생
    raise Exception(f'subscbie_topic() {name} 구독 실패')
    

def get_columns(dataset_info: str) -> List[str] | None:
    """ 
    API Gateway에 데이터셋의 컬럼 정보를 요청하는 함수.
        - dataset_info: 데이터셋의 정보(파일명 등)를 포함하는 문자열
        - return: 데이터셋의 컬럼 정보가 포함된 리스트 혹은 None
    """
    response = requests.get(url=api_gateway_url+'/data/columns/'+dataset_info)
    if response.status_code != 200:
        return None
    
    return response.json()


def get_column_values(dataset_info: str, columns: List[str]) -> pd.DataFrame:
    response = requests.post(
        url=api_gateway_url+'/csv/column-values/'+dataset_info,
        json=columns
    )
    if response.status_code == 200:
        return pd.DataFrame(response.json())
    else:
        print(f"[debug]: get_column_data 실패: {response.text}")


def get_all_values(dataset_info: str) -> pd.DataFrame | None:
    response = requests.get(api_gateway_url+'/csv/all-values/'+dataset_info)
    if response.status_code != 200:
        return None
    
    return pd.DataFrame(response.json())


def register_csv(file: str) -> Optional[str]:
    if not os.path.isfile(file):
        raise FileNotFoundError(f"File not found: {file}")
    
    response = requests.post(api_gateway_url+'/csv/register', files=[('file', open(file, 'rb'))])
    if response.status_code != 200:
        return None
    
    return response.text


def get_cardinality_ratio(dataset_info: str, column: str) -> float:
    response = requests.get(api_gateway_url+'/csv/cardinality-ratio/'+dataset_info+'/'+column)
    if response.status_code != 200:
        return None
    
    
    return float(response.json())