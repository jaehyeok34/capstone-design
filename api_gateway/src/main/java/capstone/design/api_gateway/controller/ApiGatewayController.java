package capstone.design.api_gateway.controller;

import org.springframework.web.bind.annotation.RestController;

import capstone.design.api_gateway.service.ApiGatewayService;
import org.springframework.web.bind.annotation.GetMapping;


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
