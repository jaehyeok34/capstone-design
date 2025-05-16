package team.j.api_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import team.j.api_gateway.dto.EventDTO;
import team.j.api_gateway.service.EventService;

@RestController
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }
    
    @PostMapping(value = "event", consumes = "application/json")
    public ResponseEntity<Void> event(@Valid @RequestBody EventDTO dto) throws InterruptedException {
        service.publish(dto);
        System.err.println("[debug] 이벤트 큐에 추가 완료");

        return ResponseEntity.ok().build();
    }
}
