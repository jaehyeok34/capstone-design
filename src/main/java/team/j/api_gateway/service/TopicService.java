package team.j.api_gateway.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import team.j.api_gateway.dto.TopicDTO;
import team.j.api_gateway.dto.TopicInfo;

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

    private void updateTopicTable(TopicDTO newTopic) throws Exception {
        synchronized (lock) {
            try {
                System.out.println("이것은 테스트: " + topicTablePath);
                File file = new File(topicTablePath) {{
                    createNewFile();
                }};
                
                // 토픽 테이블의 내용 읽기
                Map<String, List<TopicInfo>> topicTable = new HashMap<>() {{
                    try {
                        putAll(om.readValue(file, new TypeReference<>() {}));
                    } catch (Exception ignore) {} // 파일이 비어있거나 형식이 잘못된 경우 무시
                }};
                
                TopicInfo newTopicInfo = new TopicInfo(
                    newTopic.url(), newTopic.method(), newTopic.usePathVariable()
                );

                Optional.ofNullable(topicTable.get(newTopic.name()))
                    .ifPresentOrElse(
                        // 이미 해당 토픽이 있으면 정보 추가 혹은 갱신
                        infoList -> {
                            ListIterator<TopicInfo> iterator = infoList.listIterator();
                            while (iterator.hasNext()) {
                                TopicInfo current = iterator.next();

                                // 이미 구독된 토픽이면 새로운 정보로 갱신
                                if (current.url().equals(newTopicInfo.url())) {
                                    iterator.set(newTopicInfo);
                                    return; // 갱신 후 메소드 종료
                                }
                            }

                            // 기존에 구독된 토픽이 없으면 새롭게 추가
                            infoList.add(newTopicInfo);
                        },

                        // 토픽 테이블에 해당 토픽이 없으면 새롭게 추가
                        () -> topicTable.put(newTopic.name(), List.of(newTopicInfo))
                    );
                
                om.writeValue(file, topicTable);
            } catch (Exception e) {
                throw new Exception("updateTopicTable() 토픽 테이블 업데이트 실패: " + e.getMessage());
            }
        }
    }
}
