package team.j.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import team.j.api_gateway.service.ApiGatewayService;



@RestController
public class ApiGatewayController {
    
    @GetMapping("/")
    public String home() {
        ApiGatewayService service = new ApiGatewayService();
        return service.home();
    }
}
