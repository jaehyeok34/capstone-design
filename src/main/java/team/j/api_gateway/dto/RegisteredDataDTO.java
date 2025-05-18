package team.j.api_gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisteredDataDTO(
    @NotBlank(message = "type cannot be blank")
    String type,    // csv or DB

    @NotBlank(message = "title cannot be blank")
    String title,   

    // optional
    String path,        
    Object information
) {
    public static final String TYPE_CSV = "csv";
    public static final String TYPE_DB = "DB";
}