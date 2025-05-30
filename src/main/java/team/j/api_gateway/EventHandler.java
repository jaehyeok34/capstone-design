package team.j.api_gateway;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import team.j.api_gateway.dto.EventDTO;


@Component
public class EventHandler {
    
    private final String topicTablePath;
    private final Object lock;
    private final BlockingQueue<EventDTO> eventQueue;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public EventHandler(
        @Value("${topic.table.path}") String topicTablePath,
        @Qualifier("topicTableLock") Object lock,
        BlockingQueue<EventDTO> eventQueue
    ) {
        this.topicTablePath = topicTablePath;
        this.lock = lock;
        this.eventQueue = eventQueue;
    }

    @PostConstruct
    public void monitoring() {
        System.out.println("[debug] 이벤트 큐 모니터링 시작");
        executor.submit(() -> {
            while(true) {
                try {
                    EventDTO event = eventQueue.take();
                    handleEvent(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("[debug] 이벤트 큐 모니터링 중단");
                    break;
                }
            }
        });
    }

    private void handleEvent(EventDTO ed) {
        System.err.println("[debug] " + ed.event() + " 이벤트 처리 시작");
        try {
            File file = new File(topicTablePath);

            synchronized (lock) {
                ObjectMapper om = new ObjectMapper();
                Map<String, List<String>> topicTable = om.readValue(file, Map.class);
                
                Optional.ofNullable(topicTable.get(ed.event()))
                    .ifPresent(urls -> routing(urls.getFirst(), ed.data()));
            }
        } catch (Exception e) {
            System.err.println("[debug] " + ed.event() + "이벤트 처리 못함 " + e.getMessage());
         }
    }

    private void routing(String url, Map<String, ?> data) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }};

        HttpEntity<Map<String, ?>> entity = new HttpEntity<>(data, headers);
        restTemplate.postForObject(url, entity, Void.class); // post 요청이 잘 됐는 지, 처리가 잘 됐는지 확인 안함
    }
}