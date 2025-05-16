package team.j.api_gateway.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import team.j.api_gateway.dto.RegisterDTO;

@RestController
public class RegisterController {

    @PostMapping(value = "register", consumes = "application/json")
    public String register(@Valid @RequestBody RegisterDTO dto) {
        System.err.println("topic: " + dto.getTopic());
        System.err.println("url: " + dto.getUrl());
        return "hello world";
    }
}
