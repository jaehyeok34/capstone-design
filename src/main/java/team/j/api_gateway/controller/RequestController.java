package team.j.api_gateway.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import team.j.api_gateway.dto.RequestDTO;
import team.j.api_gateway.service.RequestService;

@RestController
public class RequestController {

    public final RequestService service;

    public RequestController(RequestService service) {
        this.service = service;
    }
    
    @PostMapping("/pre-matching-process-request")
    public ResponseEntity<Void> preMatchingProcessRequest(@Valid @RequestBody RequestDTO requestDTO) throws IOException {
        service.request(requestDTO.sourceDataTitleList(), "pii.detection.request");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/matching-process-request")
    public ResponseEntity<Void> matchingProcessReqeust(@Valid @RequestBody RequestDTO requestDTO) throws IOException {
        service.request(requestDTO.sourceDataTitleList(), "matching.request");
        return ResponseEntity.ok().build();
    }
    
}
