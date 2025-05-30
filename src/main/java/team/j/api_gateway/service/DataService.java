package team.j.api_gateway.service;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.j.api_gateway.dto.RegisteredDataDTO;


@Service
public class DataService {

    private final String registeredDataTablePath;
    private final String columnsUrl;

    private final ObjectMapper om;
    private final HttpService httpService;
    private final TableService tableService;
    private final Object lock;

    public DataService(
        @Value("${registered.data.table.path}") String registeredDataTablePath,
        @Value("${data.server.csv.columns.url}") String columnsUrl,

        ObjectMapper om, 
        HttpService httpService, 
        TableService tableService,
        @Qualifier("registeredDataTableLock") Object lock
    ) {
        this.registeredDataTablePath = registeredDataTablePath;
        this.columnsUrl = columnsUrl;

        this.om = om;
        this.httpService = httpService;
        this.tableService = tableService;
        this.lock = lock;
    }

    public List<RegisteredDataDTO> getRegisteredDatasets() throws Exception {
        synchronized (registeredDataTablePath) {
            try {
                List<RegisteredDataDTO> registeredDataList = om.readValue(new File(registeredDataTablePath), new TypeReference<>() {});
                return registeredDataList;
            } catch (Exception ignore) {
                throw new Exception("getRegisteredData() 파일을 읽는 데 실패했습니다.");
            }
        }
    }

    public List<String> getColumns(String datasetInfo) throws Exception {
        try {
            RegisteredDataDTO finded = tableService.find(lock, registeredDataTablePath, datasetInfo, RegisteredDataDTO.class);
            if (finded == null) {
                throw new Exception("getColumns() " + datasetInfo + "에 해당하는 데이터가 없습니다.");
            }

            if (finded.type().equals(RegisteredDataDTO.TYPE_CSV)) {
                return getColumnsFromCSV(finded.datasetInfo());
            } else {
                return getColumnsFromDB(finded);
            }
            
        } catch (Exception e) {
            throw new Exception("getColumns() 실패: " + e.getMessage());
        }
    }

    private List<String> getColumnsFromCSV(String datasetInfo) throws Exception {
        String url = columnsUrl + "/" + datasetInfo;
        try {
            String response = httpService.get(url);
            List<String> columns = httpService.parse(response, new TypeReference<>() {});

            return columns;
        } catch (Exception e) {
            throw new Exception("getColumnsFromCSV() 실패: " + e.getMessage());
        }
    }

    private List<String> getColumnsFromDB(RegisteredDataDTO rdd) throws Exception {
        throw new Exception("getColumnsFromDB() 메서드는 아직 구현되지 않았습니다.");
    }
}
