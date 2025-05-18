package team.j.api_gateway.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import team.j.api_gateway.dto.RegisteredDataDTO;

@Service
public class ApiGatewayService {

    public static final Object registeredDataLock = new Object();
    public static final String registeredDataPath = "resources/registered_data.json";

    public String home() {
        return "API Gateway is running";
    }

    public void saveCSV(MultipartFile csv) throws IOException {
        File dir = new File("resources/upload/") {{
            if (!exists()) {
                mkdirs();
            }
        }};
        File file = new File(dir.getAbsolutePath() + File.separator + csv.getOriginalFilename());
        csv.transferTo(file);

        updateRegisteredData(new RegisteredDataDTO(RegisteredDataDTO.TYPE_CSV, file.getName(), file.getAbsolutePath(), null));
    }

    public void updateRegisteredData(RegisteredDataDTO rd) throws IOException {
        File file = new File(registeredDataPath);

        synchronized (registeredDataLock) {
            file.createNewFile();

            List<RegisteredDataDTO> dataList = new ArrayList<>();
            ObjectMapper om = new ObjectMapper();

            try {
                dataList = om.readValue(file, new TypeReference<List<RegisteredDataDTO>>() {});
            } catch (MismatchedInputException ignore) {}
            dataList.add(rd);

            // 중복 제거
            List<RegisteredDataDTO> uniqueList = new ArrayList<>(new HashSet<>(dataList));

            // JSON 파일에 데이터 저장
            om.writeValue(file, uniqueList);
        }   
    }

    public Map<String, List<String>> getRegisteredData() throws IOException {
        ObjectMapper om = new ObjectMapper();

        synchronized (registeredDataLock) {
            List<RegisteredDataDTO> dataList = om.readValue(new File(registeredDataPath), new TypeReference<List<RegisteredDataDTO>>() {});

            return new HashMap<>() {{
                put("registered_data", dataList.stream().map(RegisteredDataDTO::title).toList());
            }};
        }
    }
}