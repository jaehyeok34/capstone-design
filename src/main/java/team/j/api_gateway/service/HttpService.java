package team.j.api_gateway.service;

import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class HttpService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper om;

    public HttpService(RestTemplate restTemplate, ObjectMapper om) {
        this.restTemplate = restTemplate;
        this.om = om;
    }

    public String get(String url) throws Exception {
        try {
            return request(url, null, "GET");
        } catch (Exception e) {
            throw new Exception("get() 실패: " + e.getMessage());
        }
    }
    
    public String post(String url, HttpEntity<?> entity) throws Exception {
        try {
            System.out.println("[debug] post() 요청 URL: " + url);
            return request(url, entity, "POST");
        } catch (Exception e) {
            throw new Exception("post() 실패: " + e.getMessage());
        }
    }

    private String request(String url, HttpEntity<?> entity, String method) throws Exception {
        try {
            List<String> allowedMethods = List.of("GET", "POST");
            if (!allowedMethods.contains(method.toUpperCase())) {
                throw new Exception(method + "는 지원하지 않음");
            }

            if ("GET".equalsIgnoreCase(method)) {
                return restTemplate.getForObject(url, String.class);
            } 

            return restTemplate.postForObject(url, entity, String.class);
        } catch (Exception e) {
            throw new Exception("request() 예외 발생: " + e.getMessage());
        }
    }

    public <T> T parse(String response, TypeReference<T> type) throws Exception {
        try {
            return om.readValue(response, type);
        } catch (Exception e) {
            throw new Exception("parse() 파싱 실패: " + e.getMessage());
        }
    }
}
