package capstone.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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
    }

    @Test
    void listTest() {
        List<Integer> list = Collections.synchronizedList(new ArrayList<>());
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(1, list.remove(0));
        assertEquals(3, list.remove(list.size() - 1));
    }
}
