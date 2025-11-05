package capstone.design;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import capstone.design.netty.NettyInitializer;
import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void nettyInitializerTest() {
        // handler를 하나도 등록하지 않으면 IllegalStateException 발생
        assertThrows(IllegalStateException.class, () -> {
            NettyInitializer.builder().build();
        });
    }

    @Test
    void optionalTest() {
        Optional<Object> o1 = Optional.ofNullable(null);
        assertThrows(NoSuchElementException.class, () -> o1.get());

        assertDoesNotThrow(() -> {"string".equals(null);});
        assertFalse("string".equals(null));
    }

    @Test
    void listTest() {
        List<Integer> list = Collections.synchronizedList(new ArrayList<>());
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(1, list.remove(0));
        assertEquals(3, list.remove(list.size() - 1));

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        String[] arr = new String[] {"a", "b", null};
        for (String s : arr) {
            map.remove(s);
        }

        assertEquals(1, map.size());
    }

    @Test
    void interruptedTest() throws InterruptedException {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("여기서 터짐");
                    break;
                }
            }
            System.out.println("빠져 나옴");
        });

        Thread.sleep(500);
        exec.shutdownNow();
        while(!exec.isTerminated()) {
            System.out.println("대기 중...");
            Thread.sleep(10);
        }
        System.out.println("종료됨");
    }

    @Test
    void nullPathTest() {
        assertDoesNotThrow(() -> {
            Long.parseLong(null);
        });
    }
}
