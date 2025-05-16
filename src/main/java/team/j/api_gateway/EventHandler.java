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
        try {
            File file = new File(RegisterService.topicTablePath);

            // RegisterService가 topic_table.json 쓰기 동안 대기
            synchronized (RegisterService.topicTableLock) {
                ObjectMapper om = new ObjectMapper();
                List<RegisterDTO> topicTable = om.readValue(file, new TypeReference<List<RegisterDTO>>() {});
                
                // 토픽 테이블에서 이벤트를 구독한 url 탐색
                // 이벤트를 구독한 url에 post 요청
                for (RegisterDTO topic : topicTable) {
                    if (topic.topic().equals(event.event())) {
                        // event.data를 topic.url에 post 요청하기
                        request(topic.url(), event.data());
                        System.err.println("[debug] " + topic.url() + "에 post 요청 완료");
                    }
                }
            }
        } catch (Exception ignore) {
            System.err.println("[debug] 이벤트 처리 못함");
         }
    }

    private void request(String url, Map<String, List<String>> data) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }};
        HttpEntity<Map<String, List<String>>> entity = new HttpEntity<>(data, headers);
        
        restTemplate.postForObject(url, entity, Void.class);
    }
}
