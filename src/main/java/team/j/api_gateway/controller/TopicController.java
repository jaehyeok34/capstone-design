package team.j.api_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import team.j.api_gateway.dto.TopicDTO;
import team.j.api_gateway.service.TopicService;

@RestController
@RequestMapping("/topic")
public class TopicController {

    private final TopicService service;

    public TopicController(TopicService service) {
        this.service = service;
    }   

    @PostMapping("/subscribe")
    public ResponseEntity<String> sbuscribeTopic(@Valid @RequestBody TopicDTO topic, HttpServletRequest request) {
        try {
            service.subscribeTopic(topic);
            System.out.println("[debug] topic_table.json에 추가 완료: " + topic.name());
            return ResponseEntity.ok("\"" + topic.name() + "\"" + " 구독 완료");
        } catch (Exception e) {
            return ResponseEntity
                .badRequest()
                .body(request.getRequestURI() + ": " + e.getMessage());
        }
    }
}
