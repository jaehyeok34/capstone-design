package team.j.api_gateway.controller;

import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import team.j.api_gateway.dto.TopicDTO;
import team.j.api_gateway.service.TopicSubscribeService;

@RestController
public class TopicSubscribeController {

    private final TopicSubscribeService service;

    public TopicSubscribeController(TopicSubscribeService service) {
        this.service = service;
    }   

    @PostMapping(value = "/subscribe-topic", consumes = "application/json")
    public ResponseEntity<String> sbuscribeTopic(@Valid @RequestBody TopicDTO td) throws IOException {
        service.subscribeTopic(td);
        System.err.println("[debug] topic_table.json에 추가 완료");
        
        return ResponseEntity.ok().body("[debug] topic_table.json에 추가 완료");
    }
}
