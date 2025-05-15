import time
import requests

class Register:

    @staticmethod
    def subscribe(register_url: str, topic: str, callback_url: str, count: int = 1, interval: int = 0) -> bool:
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
                response = requests.post(url=register_url, json={'topic': topic, 'url': callback_url})
                if response.status_code == 200:
                    print(f"[debug]: gateway에 {topic} 등록 성공")
                    return True
                
            except Exception:
                pass

            print(f"[debug]: gateway에 {topic} {i}번 째 등록 실패")
            time.sleep(interval)
            
        return False
