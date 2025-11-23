package capstone.design;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void test() {
        Map<String, Map<String, Object>> data = new HashMap<>();
        data.put("key1", Map.of("field1", 123, "field2", "345"));
        System.out.println(data);
    }
}
