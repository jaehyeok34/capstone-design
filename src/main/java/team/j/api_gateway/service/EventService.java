package team.j.api_gateway.service;

import java.util.concurrent.BlockingQueue;

import org.springframework.stereotype.Service;
import team.j.api_gateway.dto.EventDTO;

@Service
public class EventService {
    
    private final BlockingQueue<EventDTO> eventQueue;
    
    public EventService(BlockingQueue<EventDTO> eventQueue) {
        this.eventQueue = eventQueue;
    }

    public void publish(EventDTO dto) throws InterruptedException{
        eventQueue.put(dto);

        // 현재 이벤트 큐에 있는 이벤트 수를 출력
        System.err.println("[debug] 현재 이벤트 큐에 있는 이벤트 수: " + eventQueue.size());
    }
}
