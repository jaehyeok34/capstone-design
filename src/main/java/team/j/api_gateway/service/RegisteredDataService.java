package team.j.api_gateway.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import team.j.api_gateway.dto.RegisteredDataDTO;


@Service
public class RegisteredDataService {

    public List<String> getColumns(String selected) throws IOException {
        ObjectMapper om = new ObjectMapper();

        synchronized (ApiGatewayService.registeredDataLock) {
            // registeredData.json 파일을 읽어서 RegisteredDataDTO 객체 리스트로 변환
            // 변환된 리스트에서 selectedRegisteredDataList에 포함된 title을 가진 RegisteredDataDTO 객체를 필터링
            RegisteredDataDTO finded = om.readValue(
                new File(ApiGatewayService.registeredDataPath), 
                new TypeReference<List<RegisteredDataDTO>>() {}
            ).stream()
            .filter(data -> data.title().equals(selected))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No matching data found"));

            // filtered에서 type이 csv인 경우에는 getColumnsFromCSV 메서드를 호출하고,
            // type이 db인 경우에는 getColumnsFromDB 메서드를 호출하여 컬럼을 추출
            if (finded.type().equals(RegisteredDataDTO.TYPE_CSV)) {
                return getColumnsFromCSV(finded);
            } 

            return getColumnsFromDB(finded);
        }
    }

    private List<String> getColumnsFromCSV(RegisteredDataDTO selected) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(selected.path()))) {
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
}
