package team.j.api_gateway.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import team.j.api_gateway.dto.RegisterDTO;

import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class ApiGatewayController {
    
    @GetMapping("/")
    public String home() throws InterruptedException {
        return "API Gateway is running";
    }

    @PostMapping(value = "event", consumes = "application/json")
    public String event(@RequestBody Map<String, Object> entity) {

        System.err.println(entity.get("event"));
        return "hello world";
    }

    @PostMapping(value = "register", consumes = "application/json")
    public String register(@Valid @RequestBody RegisterDTO dto) {
        System.err.println("topic: " + dto.getTopic());
        System.err.println("url: " + dto.getUrl());
        return "hello world";
    }
}
