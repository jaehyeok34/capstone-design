package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void test() {
        Map<String, List<String>> map = new HashMap<>();
        
        map.put(null, List.of("v1", "v2"));
        map.put("k1", List.of("v3", "v4"));

        assertEquals("v1", map.get(null).get(0));
    }
}
