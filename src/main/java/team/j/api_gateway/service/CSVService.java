package team.j.api_gateway.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import team.j.api_gateway.dto.RegisteredDataDTO;

@Service
public class CSVService {

    private final String uploadUrl;
    private final String columnValuesUrl;
    private final String allValuesUrl;
    private final String datasetPath;

    private final HttpService httpService;
    private final TableService tableService;
    private final Object lock;

    public CSVService(
        @Value("${api.data.csv.upload}") String uploadUrl,
        @Value("${api.data.csv.column-values}") String columnValuesUrl,
        @Value("${api.data.csv.all-values}") String allValuesUrl,
        @Value("${table.path.dataset}") String datasetPath,

        HttpService httpService,
        TableService tableService,
        @Qualifier("registeredDataTableLock") Object lock
    ) {
        this.uploadUrl = uploadUrl;
        this.columnValuesUrl = columnValuesUrl;
        this.allValuesUrl = allValuesUrl;
        this.datasetPath = datasetPath;

        this.httpService = httpService;
        this.tableService = tableService;
        this.lock = lock;
    }

    public String registerCSV(MultipartFile csv) throws Exception {
        System.out.println("테스트입니다." + uploadUrl);
        try {
            String datasetInfo = requestUpload(csv);
            tableService.append(lock, datasetPath, new RegisteredDataDTO(RegisteredDataDTO.TYPE_CSV, datasetInfo, null));

            return datasetInfo;
        } catch (Exception e) {
            throw new Exception("registerCSV(): 데이터 등록 실패: " + e.getMessage());
        }
    }

    public Map<String, Object> getColumnValues(String datasetInfo, List<String> columns) throws Exception {
        try {
            RegisteredDataDTO finded = tableService.find(lock, datasetPath, datasetInfo, RegisteredDataDTO.class);
            if (finded == null) {
                throw new Exception(datasetInfo + "에 해당하는 데이터셋이 없습니다.");
            }

            String response = requestColumnValues(datasetInfo, columns);
            Map<String, Object> columnData = httpService.parse(response, new TypeReference<>() {});

            return columnData;
        } catch (Exception e) {
            throw new Exception("getCSVColumnData(): CSV 컬럼 데이터 조회 실패: " + e.getMessage());
        }
    }

    public String getAllvalues(String datasetInfo) {
        try {
            RegisteredDataDTO finded = tableService.find(lock, datasetPath, datasetInfo, RegisteredDataDTO.class);
            if (finded == null) {
                throw new Exception(datasetInfo + "에 해당하는 데이터셋이 없습니다.");
            }

            String response = requestAllValues(datasetInfo);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("getAllvalues(): CSV 전체 데이터 조회 실패: " + e.getMessage());
        }
    }

    private String requestUpload(MultipartFile csv) throws Exception {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>() {{
            add("file", csv.getResource());
        }};
        HttpHeaders headers = new HttpHeaders() {{
            setContentType(MediaType.MULTIPART_FORM_DATA);
        }};
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            return httpService.post(uploadUrl, entity);
        } catch (Exception e) {
            throw new Exception("requestUpload() CSV 파일 업로드 실패: " + e.getMessage());
        }
    }

    private String requestColumnValues(String datasetInfo, List<String> columns) throws Exception {
        String url = columnValuesUrl + "/" + datasetInfo;
        HttpHeaders headers = new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }};
        HttpEntity<List<String>> entity = new HttpEntity<>(columns, headers);

        try {
            return httpService.post(url, entity);
        } catch (Exception e) {
            throw new Exception("requestColumnValuse() CSV 컬럼 데이터 요청 실패: " + e.getMessage());
        }
    }

    
    private String requestAllValues(String datasetInfo) throws Exception {
        String url = allValuesUrl + "/" + datasetInfo;

        try {
            String response = httpService.get(url);
            return response;
        } catch (Exception e) {
            throw new Exception("requestAllValues() CSV 전체 데이터 요청 실패: " + e.getMessage());
        }
    }
}
