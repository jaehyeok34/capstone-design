package team.j.api_gateway.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ColumnDataDTO(
    @NotBlank(message = "title은 필수입니다.")
    String title,

    @NotNull(message = "columns는 필수입니다.")
    @NotEmpty(message = "columns의 요소는 필수입니다.")
    List<String> columns
) {}
