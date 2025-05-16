package team.j.api_gateway;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import team.j.api_gateway.dto.EventDTO;

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

    private void handleEvent(EventDTO dto) {
        // 실제 이벤트 처리 로직
        // 토픽 테이블을 기반으로 구독 url에 post 요청
        System.err.println("[debug] 이벤트 큐에서 이벤트 수신: " + dto);
    }
}
