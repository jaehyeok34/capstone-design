package team.j.api_gateway.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import team.j.api_gateway.dto.RegisterDTO;

@Service
public class RegisterService {
    
     public void register(RegisterDTO dto) throws IOException{
        // JSON 파일 준비
        String filePath = "topic_table.json";
        File file = new File(filePath);
        file.createNewFile(); // 파일이 이미 있으면 동작 X(false 반환, 예외 발생 X)

        // 데이터 준비
        List<RegisterDTO> registerList;
        ObjectMapper om = new ObjectMapper();
        try {
            registerList = om.readValue(file, new TypeReference<List<RegisterDTO>>() {});
        } catch (StreamReadException e) { // JSON 파일이 비어있거나 형식이 잘못된 경우(새롭게 생성된 경우)
            registerList = new ArrayList<>();
        }

        // 데이터 추가
        registerList.add(dto);

        // JSON 파일에 데이터 저장
        om.writeValue(file, registerList);
    }
}
