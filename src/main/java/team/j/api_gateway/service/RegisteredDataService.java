package team.j.api_gateway.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import team.j.api_gateway.dto.ColumnDataDTO;
import team.j.api_gateway.dto.MatchingKeyDTO;
import team.j.api_gateway.dto.RegisteredDataDTO;


@Service
public class RegisteredDataService {

    public List<String> getColumns(String selected) throws IOException {
        ObjectMapper om = new ObjectMapper();

        synchronized (ApiGatewayService.registeredDataLock) {
            // registeredData.json 파일을 읽어서 RegisteredDataDTO 객체 리스트로 변환
            // 변환된 리스트에서 selectedRegisteredDataList에 포함된 title을 가진 RegisteredDataDTO 객체를 필터링
            RegisteredDataDTO finded = find(
                om.readValue(
                    new File(ApiGatewayService.registeredDataPath),
                    new TypeReference<List<RegisteredDataDTO>>() {}
                ), 
                selected
            );

            // filtered에서 type이 csv인 경우에는 getColumnsFromCSV 메서드를 호출하고,
            // type이 db인 경우에는 getColumnsFromDB 메서드를 호출하여 컬럼을 추출
            if (finded.type().equals(RegisteredDataDTO.TYPE_CSV)) {
                return getColumnsFromCSV(finded);
            } 

            return getColumnsFromDB(finded);
        }
    }

    public Object getColumnData(ColumnDataDTO cdd) throws IOException {
        ObjectMapper om = new ObjectMapper();

        synchronized (ApiGatewayService.registeredDataLock) {
            RegisteredDataDTO finded = find(
                om.readValue(
                    new File(ApiGatewayService.registeredDataPath),
                    new TypeReference<List<RegisteredDataDTO>>() {}
                ), 
                cdd.title()
            );

            if (finded.type().equals(RegisteredDataDTO.TYPE_CSV)) {
                return getColumnDataFromCSV(finded.csvFilePath(), cdd.columns());
            } 
            
            // TODO: DB에서 컬럼 데이터 추출 로직 구현
            // 현재는 미구현 상태

            return null;
        }
    }

    public void updateCSV(MatchingKeyDTO mkd) throws IOException {
        ObjectMapper om = new ObjectMapper();
        
        synchronized (ApiGatewayService.registeredDataLock) {
            RegisteredDataDTO finded = find(
                om.readValue(
                    new File(ApiGatewayService.registeredDataPath),
                    new TypeReference<List<RegisteredDataDTO>>() {}
                ), 
                mkd.selectedRegisteredDataTitle()
            );

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders() {{
                setContentType(MediaType.APPLICATION_JSON);
            }};
            String url = "http://localhost:1789/update-csv";

            // PyUpdateCSVDTO에 맞춰서 작성
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                new HashMap<>() {{
                    put("csvFilePath", finded.csvFilePath());
                    put("matchingKeyData", mkd.matchingKeyData());
                }}, 
                headers
            );
            
            restTemplate.postForObject(url, entity, String.class);
        }

    }

    private RegisteredDataDTO find(List<RegisteredDataDTO> list, String selected) throws IOException {
        return list.stream()
            .filter(data -> data.title().equals(selected))
            .findFirst()
            .orElseThrow(() -> new IOException("No matching data found"));
    }

    private List<String> getColumnsFromCSV(RegisteredDataDTO selected) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(selected.csvFilePath()))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("Empty CSV file");
            }
            
            return List.of(headerLine.split(","));
        } catch (IOException e) {
            throw new IOException("Error reading CSV file", e);
        }
    }

    private List<String> getColumnsFromDB(RegisteredDataDTO selectedRegisteredData) {
        // DB에서 컬럼을 추출하는 로직을 구현해야 함
        // 현재는 미구현 상태
        return null;
    }

    private Map<String, Object> getColumnDataFromCSV(String csvFilePath, List<String> columns) throws IOException {
        Map<String, Object> requestDTO = new HashMap<>() {{
            put("csvFilePath", csvFilePath);
            put("columns", columns);
        }};

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }};
        String url = "http://localhost:1789/get-column-data";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestDTO, headers);
        
        return restTemplate.postForObject(url, entity, Map.class);
    }
}
