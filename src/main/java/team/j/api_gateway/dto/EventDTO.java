package team.j.api_gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record EventDTO(
    @NotBlank(message = "event는 필수 항목 입니다.")
    String name,

    String pathVariable,
    String jsonData // JSON을 문자열로 직렬화하여 전달(python ex: 'jsonData': json.dumps(data))
) {}   
