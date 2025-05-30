package team.j.api_gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record EventDTO(
    @NotBlank(message = "event는 필수 항목 입니다.")
    String name,

    String pathVariable,
    String jsonData
) {}   
