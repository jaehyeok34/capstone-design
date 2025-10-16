package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void test() {
        Map<String, String> map = new HashMap<>();

        assertDoesNotThrow(() -> map.get(null));
        assertEquals(null, map.get(null));
    }
}
