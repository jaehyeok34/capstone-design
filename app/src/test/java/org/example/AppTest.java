package org.example;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.example.netty.NettyInitializer;
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
}
