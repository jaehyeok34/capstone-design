package team.j.api_gateway.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import team.j.api_gateway.dto.RegisteredDataDTO;
import team.j.api_gateway.dto.EventDTO;


@Service
public class RequestService {
    
    public void request(List<String> selectedRegisteredData, String event) throws IOException {
        ObjectMapper om = new ObjectMapper();

        synchronized (ApiGatewayService.registeredDataLock) {
            // registeredData.json 파일을 읽어서 RegisteredDataDTO 객체 리스트로 변환
            List<RegisteredDataDTO> registeredData = om.readValue(new File(ApiGatewayService.registeredDataPath), new TypeReference<List<RegisteredDataDTO>>() {});

            // selectedRegisteredDataList에 포함된 title을 가진 RegisteredDataDTO 객체를 필터링
            List<String> filtered = selectedRegisteredData.stream()
                .filter(selected -> registeredData.stream().anyMatch(data -> data.title().equals(selected)))
                .toList();

            if (filtered.isEmpty()) {
                throw new IllegalArgumentException("No matching data found");
            }
            
            // /event로 POST 요청
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders() {{
                setContentType(MediaType.APPLICATION_JSON);
            }};

            HttpEntity<EventDTO> request = new HttpEntity<>(
                // PIIDetectionDTO 혹은 MatchingDTO와 일치해야 함
                new EventDTO(event, Map.of("selectedRegisteredData", filtered)),
                headers
            );

            String url = "http://localhost:1780/event";
            restTemplate.postForEntity(url, request, Void.class);
        }
    }
}
