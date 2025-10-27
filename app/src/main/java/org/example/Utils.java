package org.example;

import io.netty.buffer.ByteBuf;

public class Utils {
    
    /**
     * 기본적으로 null 체크
     * 일부 타입은 추가 검증 수행
     * IllegalStateException 발생
     */
    public static void validate(Object... objects) {
        for (Object object : objects) {
            if (object == null) {
                throw new IllegalStateException("null");
            }

            if (object instanceof String string && string.isEmpty()) {
                throw new IllegalStateException("empty string");
            } else if (object instanceof ByteBuf buf && (buf.refCnt() <= 0 || buf.readableBytes() == 0)) {
                throw new IllegalStateException("released or empty buf");
            } else if (object instanceof byte[] arr && arr.length == 0) {
                throw new IllegalStateException("empty byte array");
            }
         }
    }
}
