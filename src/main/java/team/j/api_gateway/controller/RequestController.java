package team.j.api_gateway.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import team.j.api_gateway.service.RequestService;

@RestController
public class RequestController {

    public final RequestService service;

    public RequestController(RequestService service) {
        this.service = service;
    }
    
    @PostMapping("/matching-key-routine-request")
    public ResponseEntity<String> matchingKeyRoutineRequest(@RequestBody List<String> selectedRegisteredData) throws IOException {
        if (selectedRegisteredData.isEmpty()) {
            return ResponseEntity.badRequest().body("[debug] 데이터가 없음");
        }

        service.request(selectedRegisteredData, "pii.detection.request");
        return ResponseEntity.ok().body("[debug] 결합키 생성 과정 요청 완료");
    }

    @PostMapping("/matching-routine-request")
    public ResponseEntity<String> matchingRoutineRequest(@RequestBody List<String> selectedRegisteredData) throws IOException {
        if (selectedRegisteredData.isEmpty()) {
            return ResponseEntity.badRequest().body("[debug] 데이터가 없음");
        }
        
        service.request(selectedRegisteredData, "matching.request");
        return ResponseEntity.ok().body("[debug] 결합 과정 요청 완료");
    }
    
}
