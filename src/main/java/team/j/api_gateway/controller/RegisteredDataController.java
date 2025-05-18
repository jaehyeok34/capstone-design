package team.j.api_gateway.controller;

import org.springframework.web.bind.annotation.RestController;

import team.j.api_gateway.service.RegisteredDataService;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
public class RegisteredDataController {

    private final RegisteredDataService service;

    public RegisteredDataController(RegisteredDataService service) {
        this.service = service;
    }

    @PostMapping("/get-columns")
    public ResponseEntity<Object> getColumns(@RequestBody String selected) throws IOException {
        if (selected.isEmpty() || selected.isBlank()) {
            return ResponseEntity.badRequest().body("[debug] 데이터가 없음");
        }

        System.err.println("[debug] " + selected );

        List<String> columns = service.getColumns(selected);
        if (columns == null || columns.isEmpty()) {
            return ResponseEntity.badRequest().body("[debug] 컬럼 추출 실패");
        }

        return ResponseEntity.ok(columns);
    }
}
