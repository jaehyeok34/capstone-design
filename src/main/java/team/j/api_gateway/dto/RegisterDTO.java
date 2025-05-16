package team.j.api_gateway.dto;

import org.hibernate.validator.constraints.URL;
import jakarta.validation.constraints.NotBlank;

public class RegisterDTO {
    
    @NotBlank(message = "Topic cannot be blank")
    private String topic;

    @NotBlank(message = "url cannot be blank")
    @URL(message = "url must be a valid URL")
    private String url;

    public String getTopic() {
        return topic;
    }

    public String getUrl() {
        return url;
    }
}
