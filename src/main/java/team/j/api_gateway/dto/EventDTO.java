package team.j.api_gateway.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record EventDTO(
    @NotBlank(message = "event cannot be blank")
    String event,

    Map<String, List<String>> data
) {}    
