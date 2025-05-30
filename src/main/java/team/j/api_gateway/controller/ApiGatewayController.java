package team.j.api_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import team.j.api_gateway.service.ApiGatewayService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



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

    @PostMapping("/register-database")
    public ResponseEntity<Void> registerDatabase(@RequestBody String entity) {
        // Todo: 나중에 개발할 거임
        return ResponseEntity.ok().build();
    }
}
