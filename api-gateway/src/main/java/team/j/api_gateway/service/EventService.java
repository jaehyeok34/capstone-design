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

    public void publish(EventDTO event) throws Exception {
        try {
            eventQueue.put(event);
            System.out.println("[debug] publish() 이벤트 적재 성공(" + event.name() + ")");
        } catch (InterruptedException e) {
            throw new Exception("publish() 이벤트 적재 실패(" + event.name() + ")");
        }
    }
}
