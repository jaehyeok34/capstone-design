package team.j.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import team.j.api_gateway.service.ApiGatewayService;



@RestController
public class ApiGatewayController {

    private final ApiGatewayService service;

    public ApiGatewayController(ApiGatewayService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String home() {
        return service.home();
    }
}
