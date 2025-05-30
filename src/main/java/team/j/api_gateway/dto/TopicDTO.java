package team.j.api_gateway.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TopicDTO(
    @NotBlank(message = "name은 필수 항목 입니다.")
    String name,
    
    @NotBlank(message = "url은 필수 항목 입니다.")
    @URL(message = "url은 반드시 유효한 URL 형식이어야 합니다.")
    String url,

    @NotBlank(message = "method는 필수 항목 입니다.")
    @Pattern(regexp = "GET|POST", message = "GET 또는 POST 메소드만 허용됩니다.")
    String method,

    @NotNull(message = "usePathVariable은 필수 항목 입니다.")
    boolean usePathVariable
) {}