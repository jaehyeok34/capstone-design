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
            List<RegisteredDataDTO> registeredData = om.readValue(new File(ApiGatewayService.registeredDataPath), new TypeReference<List<RegisteredDataDTO>>() {});
            List<String> filtered = selectedRegisteredData.stream()
                .filter(selected -> registeredData.stream().anyMatch(data -> data.title().equals(selected)))
                .toList();

            if (filtered.isEmpty()) {
                throw new IllegalArgumentException("No matching data found");
            }

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders() {{
                setContentType(MediaType.APPLICATION_JSON);
            }};

            HttpEntity<EventDTO> request = new HttpEntity<>(
                new EventDTO(event, Map.of("data_list", filtered)),
                headers
            );

            String url = "http://localhost:1780/event";
            restTemplate.postForEntity(url, request, Void.class);
        }
    }
}
