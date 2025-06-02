package team.j.api_gateway;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import team.j.api_gateway.dto.EventDTO;
import team.j.api_gateway.dto.TopicInfo;
import team.j.api_gateway.service.HttpService;


@Component
public class EventHandler {
    
    private final String topicTablePath;
    private final Object lock;
    private final ObjectMapper om;
    private final HttpService httpService;
    private final BlockingQueue<EventDTO> eventQueue;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public EventHandler(
        @Value("${topic.table.path}") String topicTablePath,
        @Qualifier("topicTableLock") Object lock,
        ObjectMapper om,
        HttpService httpService,
        BlockingQueue<EventDTO> eventQueue
    ) {
        this.topicTablePath = topicTablePath;
        this.lock = lock;
        this.om = om;
        this.httpService = httpService;
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
                    System.out.println("[debug] 이벤트 큐 모니터링 중단");
                    break;
                }
            }
        });
    }

    private void handleEvent(EventDTO event) {
        System.out.println("[debug] " + event.name() + " 이벤트 처리 시작");
        try {
            File file = new File(topicTablePath);

            synchronized (lock) {
                Map<String, List<TopicInfo>> topicTable = om.readValue(file, new TypeReference<>() {});
                List<TopicInfo> topicInfoList = topicTable.get(event.name());
                if (topicInfoList == null) {
                    throw new Exception(event.name() + "을 구독하는 서비스가 없습니다.");
                }

                String response = routing(topicInfoList, event);
                System.out.println("[debug] " + event.name() + " 이벤트 처리 결과: " + response);
            }
        } catch (Exception e) {
            System.out.println("[debug] " + event.name() + " 이벤트 처리 못함: " + e.getMessage());
         }
    }

    private String routing(List<TopicInfo> topicInfoList, EventDTO event) throws Exception {
        try {
            if ( 
                topicInfoList.getFirst().usePathVariable() &&
                (event.pathVariable() == null || event.pathVariable().isBlank())
            ) {
                throw new Exception("path variable이 필요합니다.");
            }

            String url = topicInfoList.getFirst().url() + (topicInfoList.getFirst().usePathVariable() ? ("/" + event.pathVariable()) : "");
            HttpHeaders headers = new HttpHeaders() {{
                setContentType(MediaType.APPLICATION_JSON);
            }};
            
            if (topicInfoList.getFirst().method().equals("GET")) {
                String response = httpService.get(url);
                return response;
            }
            
            HttpEntity<String> entity = new HttpEntity<>(event.jsonData(), headers);
            String response = httpService.post(url, entity);

            return response;
        } catch (Exception e) {
            throw new Exception("routing() 이벤트 처리 중 오류 발생: " + e.getMessage());
        }
    }
}