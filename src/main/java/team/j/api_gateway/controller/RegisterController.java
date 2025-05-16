package team.j.api_gateway.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import team.j.api_gateway.dto.RegisterDTO;
import team.j.api_gateway.service.RegisterService;

@RestController
public class RegisterController {

    @PostMapping(value = "register", consumes = "application/json")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterDTO dto) throws IOException {
        RegisterService service = new RegisterService();
        service.register(dto);

        System.err.println("[debug] topic_table.json에 추가 완료");
        
        return ResponseEntity.ok().build();
    }
}
