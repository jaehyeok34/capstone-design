package team.j.api_gateway.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MatchingKeyDTO(
    @NotBlank(message = "[debug] null이 되면 안됨")
    String selectedRegisteredDataTitle,

    @NotNull(message = "[debug] null이 되면 안됨 알겠지?")
    Map<String, Object> matchingKeyData // pd.DataFrame
) {}