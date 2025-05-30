package team.j.api_gateway.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import team.j.api_gateway.dto.TopicDTO;

@Service
public class TopicService {

    private final String topicTablePath;
    private final Object lock;
    private final ObjectMapper om;

    public TopicService(
        @Value("${topic.table.path}") String topicTablePath,
        @Qualifier("topicTableLock") Object lock,
        ObjectMapper om
    ) {
        this.topicTablePath = topicTablePath;
        this.lock = lock;
        this.om = om;
    }
    
    public void subscribeTopic(TopicDTO td) throws Exception  {
        try {
            updateTopicTable(td);
        } catch (Exception e) {
            throw new Exception("subscribeTopic() 토픽 구독 실패: " + e.getMessage());
        }
    }

    private void updateTopicTable(TopicDTO td) throws Exception {
        synchronized (lock) {
            try {
                File file = new File(topicTablePath) {{
                    createNewFile();
                }};

                // JSON 파일에서 기존 데이터를 읽어옴
                Map<String, List<String>> topicTable = new HashMap<>() {{
                    try {
                        putAll(om.readValue(file, Map.class));
                    } catch (Exception ignore) {} // 파일이 비어있거나 형식이 잘못된 경우 무시
                }};

                // 추가하고자 하는 토픽이 테이블에 존재하는지 확인
                Optional.ofNullable(topicTable.get(td.topic()))
                    .ifPresentOrElse(
                        // 존재한다면, url 추가(중복 시 추가X)
                        urls -> {   
                            if (!urls.contains(td.url())) {
                                urls.add(td.url());
                            }
                        }, 

                        // 존재하지 않는다면, 새롭게 추가
                        () -> topicTable.put(td.topic(), List.of(td.url()))
                    );
                    
                om.writeValue(file, topicTable);
            } catch (Exception e) {
                throw new Exception("updateTopicTable() 토픽 테이블 업데이트 실패: " + e.getMessage());
            }
        }
    }
}
