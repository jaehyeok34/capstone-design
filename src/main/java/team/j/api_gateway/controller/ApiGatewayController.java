package team.j.api_gateway.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import team.j.api_gateway.service.ApiGatewayService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class ApiGatewayController {

    private final ApiGatewayService service;

    public ApiGatewayController(ApiGatewayService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String home() {
        ApiGatewayService service = new ApiGatewayService();
        return service.home();
    }

    @PostMapping("/register-database")
    public ResponseEntity<Void> registerDatabase(@RequestBody String entity) {
        // Todo: 나중에 개발할 거임

        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/upload-csv")
    public ResponseEntity<Void> uploadCSV(@RequestParam("file") MultipartFile csv) throws IOException {
        if (csv.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        service.saveCSV(csv); 

        return ResponseEntity.ok().build();
    }

    @GetMapping("/get-data-list")
    public ResponseEntity<Map<String, List<String>>> getDataList() throws IOException {
        return ResponseEntity.ok(service.getDataList());
    }
}
