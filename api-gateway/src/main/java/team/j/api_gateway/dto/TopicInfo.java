package team.j.api_gateway.dto;

public record TopicInfo(
    String url,
    String method,
    boolean usePathVariable
) {}
