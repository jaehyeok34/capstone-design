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
    
    @PostMapping(value = "/event", consumes = "application/json")
    public ResponseEntity<String> event(@Valid @RequestBody EventDTO ed) throws InterruptedException {
        service.publish(ed);
        return ResponseEntity.ok().body("[debug] 이벤트 적재 완료");
    }
}
