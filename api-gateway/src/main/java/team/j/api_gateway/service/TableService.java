package team.j.api_gateway.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TableService {

    private final ObjectMapper om;

    public TableService(ObjectMapper om) {
        this.om = om;
    }

    /**
     * JSON 파일에 완전히 새로운 데이터를 추가한다.
     * @param <T> RegisteredDataDTO | TopicDTO
     * @param lock json 파일에 대한 동기화 객체
     * @param path json 파일 경로
     * @param data 추가할 데이터
     * @throws Exception 파일 갱신 실패 시
     */
    public <T> void append(Object lock, String path, T data) throws Exception {
        synchronized (lock) {
            try {
                File file = new File(path) {{
                    createNewFile();
                }};
                JavaType type = om.getTypeFactory().constructCollectionType(List.class, data.getClass());
                List<T> dataList = new ArrayList<>() {{
                    try {
                        addAll(om.readValue(file, type));
                    } catch (StreamReadException | DatabindException ignore) {}
                    add(data);
                }};

                // 중복 제거
                List<T> uniqueList = new ArrayList<>(new HashSet<>(dataList));
    
                // JSON 파일에 데이터 저장
                om.writeValue(file, uniqueList);
            } catch (Exception e) {
                throw new Exception("update() 파일 갱신 실패: " + e.getMessage());
            }
        }   
    }

    public <T> T find(Object lock, String path, String target, Class<T> type) throws Exception {
        File file = new File(path);
        synchronized (lock) {
            try {
                JavaType t = om.getTypeFactory().constructCollectionType(List.class, type);
                List<T> table = om.readValue(file, t);
                for (T item : table) {
                    for (var field : item.getClass().getDeclaredFields()) {
                        field.setAccessible(true);
                        Object value = field.get(item);
                        if (value != null && value.toString().equals(target)) {
                            return item;
                        }
                    }
                }

                return null;
            } catch (Exception e) {
                throw new Exception("search() 파일 읽기 실패");
            }
        }
    }
}
