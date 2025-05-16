package team.j.api_gateway;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import team.j.api_gateway.dto.EventDTO;
import team.j.api_gateway.dto.RegisterDTO;
import team.j.api_gateway.service.RegisterService;


@Component
public class EventHandler {
    
    private final BlockingQueue<EventDTO> eventQueue;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public EventHandler(BlockingQueue<EventDTO> eventQueue) {
        this.eventQueue = eventQueue;
    }

    @PostConstruct
    public void monitoring() {
        System.err.println("[debug] 이벤트 큐 모니터링 시작");

        executor.submit(() -> {
            while(true) {
                try {
                    EventDTO event = eventQueue.take();
                    handleEvent(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("[debug] 이벤트 큐 모니터링 중단");
                    break;
                }
            }
        });
    }

    private void handleEvent(EventDTO event) {
        System.err.println("[debug] " + event.event() + " 이벤트 처리 시작");
        try {
            File file = new File(RegisterService.topicTablePath);

            // RegisterService가 topic_table.json 쓰기 동안 대기
            synchronized (RegisterService.topicTableLock) {
                ObjectMapper om = new ObjectMapper();
                List<RegisterDTO> topicTable = om.readValue(file, new TypeReference<List<RegisterDTO>>() {});
                
                // 토픽 테이블에서 이벤트를 구독한 서비스 찾기
                for (RegisterDTO topic : topicTable) {
                    if (topic.topic().equals(event.event())) { // 구독한 서비스 찾음
                        // event.data를 topic.url에 post 요청하기
                        request(topic.url(), event.data(), topic.requirePiiData());
                        System.err.println("[debug] " + topic.url() + "에 post 요청 완료");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[debug] " + event.event() + "이벤트 처리 못함 " + e.getMessage());
         }
    }

    private void request(String url, Map<String, Object> data, Boolean requirePiiData) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }};
        String dataServerUrl = "http://localhost:1789/get-pii-data";
        
        // 민감정보가 필요한 경우, 데이터 서버에서 데이터를 가져옴
        if (requirePiiData) {
            data.put("piiData", restTemplate.getForObject(dataServerUrl, Map.class));
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);
        restTemplate.postForObject(url, entity, Void.class); // post 요청이 잘 됐는 지, 처리가 잘 됐는지 확인 안함
    }
}
