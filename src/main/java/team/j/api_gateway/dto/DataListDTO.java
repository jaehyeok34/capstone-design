package team.j.api_gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record DataListDTO(
    @NotBlank(message = "type cannot be blank")
    String type,

    @NotBlank(message = "title cannot be blank")
    String title,

    String path,

    Object information
) {
    public static final String TYPE_CSV = "csv";
    public static final String TYPE_JSON = "json";
}