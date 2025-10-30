package capstone.design.api_gateway.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import capstone.design.api_gateway.service.ApiGatewayService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


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

    @PostMapping("/api/convert")
    public ResponseEntity<Map<String, String>> convertFile(@RequestParam("file") MultipartFile file) throws IOException {
        System.out.println("[debug] /api/convert");
        byte[] markdown = service.convertFile(file);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("markdown", new String(markdown, StandardCharsets.UTF_8)));
    }

    @PostMapping("/api/find-join-keys")
    public ResponseEntity<Map<?, ?>> findJoinKeys(@RequestParam("files") MultipartFile[] files) throws IOException {
        if (files.length != 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일 두 개 필요"));
        }

        var result = service.findJoinKeys(files);
        if (result == null) {
            return ResponseEntity.internalServerError().body(Map.of("error", "결합 키 탐색 실패"));
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(result);
    }
}

