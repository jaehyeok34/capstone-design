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
import team.j.api_gateway.dto.DataListDTO;

@Service
public class ApiGatewayService {

    public static final Object dataListLock = new Object();
    public static final String dataListPath = "resources/data_list.json";

    public String home() {
        return "API Gateway is running";
    }

    public void saveCSV(MultipartFile csv) throws IOException {
        File dir = new File("resources/") {{
            if (!exists()) {
                mkdirs();
            }
        }};
        File file = new File(dir.getAbsolutePath() + File.separator + csv.getOriginalFilename());
        csv.transferTo(file);

        updateDataList(new DataListDTO(DataListDTO.TYPE_CSV, file.getName(), file.getAbsolutePath(), null));
    }

    public void updateDataList(DataListDTO data) throws IOException {
        File file = new File(dataListPath);

        synchronized (dataListLock) {
            file.createNewFile();

            List<DataListDTO> dataList = new ArrayList<>();
            ObjectMapper om = new ObjectMapper();

            try {
                dataList = om.readValue(file, new TypeReference<List<DataListDTO>>() {});
            } catch (MismatchedInputException ignore) {}
            dataList.add(data);

            // 중복 제거
            List<DataListDTO> uniqueList = new ArrayList<>(new HashSet<>(dataList));

            // JSON 파일에 데이터 저장
            om.writeValue(file, uniqueList);
        }   
    }

    public Map<String, List<String>> getDataList() throws IOException {
        ObjectMapper om = new ObjectMapper();

        synchronized (dataListLock) {
            List<DataListDTO> dataList = om.readValue(new File(dataListPath), new TypeReference<List<DataListDTO>>() {});

            return new HashMap<>() {{
                put("data_list", dataList.stream().map(DataListDTO::title).toList());
            }};
        }
    }
}