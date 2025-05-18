package team.j.api_gateway.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import team.j.api_gateway.dto.RegisterDTO;

@Service
public class RegisterService {

    public static final Object topicTableLock = new Object();
    public static final String topicTablePath = "resources/topic_table.json";
    
     public void register(RegisterDTO dto) throws IOException{
        // JSON 파일 준비
        File file = new File(topicTablePath);

        // EventHandler가 topic_table.json을 읽을 동안 대기
        synchronized (topicTableLock) {
            file.createNewFile(); // 파일이 이미 있으면 동작 X(false 반환, 예외 발생 X)

            // 데이터 준비
            List<RegisterDTO> registerList = new ArrayList<>();
            ObjectMapper om = new ObjectMapper();

            // 데이터 추가
            try {
                registerList = om.readValue(file, new TypeReference<List<RegisterDTO>>() {});
            } catch (MismatchedInputException ignore) {}
            registerList.add(dto);

            // 중복 제거
            List<RegisterDTO> uniqueList = new ArrayList<>(new HashSet<>(registerList));

            // JSON 파일에 데이터 저장
            om.writeValue(file, uniqueList);
        }
    }
}
