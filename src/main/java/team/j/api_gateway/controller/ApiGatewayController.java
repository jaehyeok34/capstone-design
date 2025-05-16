package team.j.api_gateway.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import team.j.api_gateway.service.ApiGatewayService;

import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class ApiGatewayController {
    
    @GetMapping("/")
    public String home() {
        ApiGatewayService service = new ApiGatewayService();
        return service.home();
    }

    @PostMapping(value = "event", consumes = "application/json")
    public String event(@RequestBody Map<String, Object> entity) {

        System.err.println(entity.get("event"));
        return "hello world";
    }
}
