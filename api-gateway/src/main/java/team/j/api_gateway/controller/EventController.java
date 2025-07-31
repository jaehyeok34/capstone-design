package team.j.api_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import team.j.api_gateway.dto.EventDTO;
import team.j.api_gateway.service.EventService;

@RestController
@RequestMapping("/event")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }
    
    @PostMapping("/publish")
    public ResponseEntity<String> publish(@Valid @RequestBody EventDTO event, HttpServletRequest request) {
        try {
            service.publish(event);
            return ResponseEntity.ok("\"" + event.name() + "\"" + " 적재 완료");
        } catch (Exception e) {
            return ResponseEntity
                .badRequest()
                .body(request.getRequestURI() + ": " + e.getMessage());
        }
    }
}
