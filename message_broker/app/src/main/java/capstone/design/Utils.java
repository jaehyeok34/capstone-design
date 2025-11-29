package capstone.design;

import java.util.Map;

import io.netty.buffer.ByteBuf;

public class Utils {

    public static final int MAGIC = 0x6B3FA0FF;
    
    /**
     * 기본적으로 null 체크
     * 일부 타입은 추가 검증 수행
     * IllegalStateException 발생
     */
    public static void validate(Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                throw new IllegalStateException("argument " + i + " is null");
            }

            Object object = objects[i];
            if (object instanceof String string && string.isEmpty()) {
                throw new IllegalStateException("empty string");
            } else if (object instanceof ByteBuf buf && (buf.refCnt() <= 0 || buf.readableBytes() == 0)) {
                throw new IllegalStateException("released or empty buf");
            } else if (object instanceof byte[] arr && arr.length == 0) {
                throw new IllegalStateException("empty byte array");
            } else if (object instanceof Map<?, ?> map && map.isEmpty()) {
                throw new IllegalStateException("empty map");
            }
        }
    }

    public static boolean isValid(Object... objects) {
        try {
            validate(objects);
            return true;
        } catch (Exception e) {
            System.err.println("? Utils.isValid(): " + e);
        }

        return false;   
    }
}
