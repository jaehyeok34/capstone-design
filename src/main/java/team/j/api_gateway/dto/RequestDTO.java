package team.j.api_gateway.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record RequestDTO(
    @NotEmpty(message = "sourceDataTitleList cannot be empty")
    List<String> sourceDataTitleList
) {}
