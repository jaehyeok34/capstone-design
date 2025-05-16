package team.j.api_gateway.dto;

import org.hibernate.validator.constraints.URL;
import jakarta.validation.constraints.NotBlank;

/**
 * topic: 이벤트명
 * url: 이벤트를 처리하는 서버의 URL
 * requireData: 이벤트 발생 시 민감정보 데이터 전송 여부
 */
public record RegisterDTO(
    @NotBlank(message = "Topic cannot be blank")
    String topic,
    
    @NotBlank(message = "url cannot be blank")
    @URL(message = "url must be a valid URL")
    String url,

    boolean requirePiiData
) {}

// public class RegisterDTO {
    
//     @NotBlank(message = "Topic cannot be blank")
//     private String topic;

//     @NotBlank(message = "url cannot be blank")
//     @URL(message = "url must be a valid URL")
//     private String url;

//     public String getTopic() {
//         return topic;
//     }

//     public String getUrl() {
//         return url;
//     }
// }
