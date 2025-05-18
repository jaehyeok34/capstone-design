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

    public void publish(EventDTO ed) throws InterruptedException{
        eventQueue.put(ed);

        // 현재 이벤트 큐에 있는 이벤트 수를 출력
        System.err.println("[debug] 현재 이벤트 큐에 있는 이벤트 수: " + eventQueue.size());
    }
}
