package team.j.api_gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisteredDataDTO(
    @NotBlank(message = "type cannot be blank")
    @Pattern(regexp = "csv|db", message = "type must be either 'csv' or 'db'")
    String type,    // csv or db

    @NotBlank(message = "datasetInfo cannot be blank")
    String datasetInfo,   

    // optional
    Object dbConnectionInfo     // DB connection information
) {
    public static final String TYPE_CSV = "csv";
    public static final String TYPE_DB = "db";
}