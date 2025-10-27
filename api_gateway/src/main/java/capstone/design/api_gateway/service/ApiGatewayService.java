package capstone.design.api_gateway.service;

import org.springframework.stereotype.Service;

@Service
public class ApiGatewayService {
    
    public String home() {
        return "api gateway";
    }
}
